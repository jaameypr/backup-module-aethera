package it.pruefert.backupmodule.service;

import it.pruefert.backupmodule.model.BackupJob;
import it.pruefert.backupmodule.model.BackupRequest;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private static final Map<String, List<String>> COMPONENT_PATHS = Map.of(
            "world", List.of("world", "world_nether", "world_the_end"),
            "config", List.of("server.properties", "bukkit.yml", "spigot.yml", "paper.yml", "config"),
            "mods", List.of("mods"),
            "plugins", List.of("plugins"),
            "datapacks", List.of("world/datapacks")
    );

    private static final DateTimeFormatter FILENAME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss-SSS'Z'").withZone(ZoneOffset.UTC);

    private final ConcurrentHashMap<String, BackupJob> jobs = new ConcurrentHashMap<>();
    private final CallbackService callbackService;
    private final PaperviewService paperviewService;

    @Value("${backup.data-dir}")
    private String dataDir;

    @Value("${backup.backup-dir}")
    private String backupDir;

    public BackupService(CallbackService callbackService, PaperviewService paperviewService) {
        this.callbackService = callbackService;
        this.paperviewService = paperviewService;
    }

    public BackupJob submitJob(BackupRequest request) {
        String jobId = UUID.randomUUID().toString();
        var job = new BackupJob(jobId, request);
        jobs.put(jobId, job);
        log.info("Job {} submitted for server {}", jobId, request.serverIdentifier());
        return job;
    }

    public Optional<BackupJob> getJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public Collection<BackupJob> getAllJobs() {
        return Collections.unmodifiableCollection(jobs.values());
    }

    @Async("backupExecutor")
    public void executeBackup(BackupJob job) {
        job.setStatus(BackupJob.Status.IN_PROGRESS);
        log.info("Job {} started — packing {} for server {}",
                job.getJobId(), job.getComponents(), job.getServerIdentifier());

        try {
            Path serverDataPath = Path.of(dataDir, job.getServerIdentifier());
            if (!Files.isDirectory(serverDataPath)) {
                throw new IOException("Server data directory not found: " + serverDataPath);
            }

            Path serverBackupDir = Path.of(backupDir, job.getServerId());
            Files.createDirectories(serverBackupDir);

            String safeName = job.getServerName().replaceAll("[^a-zA-Z0-9._-]", "_");
            String filename = FILENAME_FMT.format(Instant.now()) + "-" + safeName + ".tar.gz";
            Path archivePath = serverBackupDir.resolve(filename);

            long size = packArchive(archivePath, serverDataPath, job.getComponents());

            job.setFilename(filename);
            job.setPath(archivePath.toString());
            job.setSize(size);

            // Upload to Paperview if configured
            if (paperviewService.isAvailable()) {
                try {
                    var shareResult = paperviewService.uploadBackup(archivePath, filename, job.getServerName());
                    job.setShareUrl(shareResult.shareUrl());
                    job.setShareId(shareResult.shareId());
                    log.info("Job {} — uploaded to Paperview: {}", job.getJobId(), shareResult.shareUrl());
                } catch (Exception e) {
                    log.warn("Job {} — Paperview upload failed (backup still saved to disk): {}",
                            job.getJobId(), e.getMessage());
                }
            }

            job.setStatus(BackupJob.Status.COMPLETED);
            log.info("Job {} completed — {} ({} bytes)", job.getJobId(), filename, size);

            callbackService.notifyAethera(job);

        } catch (Exception e) {
            job.setStatus(BackupJob.Status.FAILED);
            job.setError(e.getMessage());
            log.error("Job {} failed: {}", job.getJobId(), e.getMessage(), e);

            callbackService.notifyAethera(job);
        }
    }

    private long packArchive(Path archivePath, Path serverDataPath, List<String> components) throws IOException {
        try (OutputStream fos = Files.newOutputStream(archivePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gzos)) {

            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

            for (String component : components) {
                List<String> paths = COMPONENT_PATHS.getOrDefault(component, List.of());
                for (String relPath : paths) {
                    Path fullPath = serverDataPath.resolve(relPath);
                    if (Files.exists(fullPath)) {
                        addToArchive(tar, fullPath, relPath);
                    }
                }
            }

            tar.finish();
        }

        return Files.size(archivePath);
    }

    private void addToArchive(TarArchiveOutputStream tar, Path source, String entryName) throws IOException {
        if (Files.isDirectory(source)) {
            try (Stream<Path> walk = Files.walk(source)) {
                walk.forEach(path -> {
                    try {
                        String name = entryName + "/" + source.relativize(path);
                        if (Files.isDirectory(path)) {
                            var entry = new TarArchiveEntry(path.toFile(), name + "/");
                            tar.putArchiveEntry(entry);
                            tar.closeArchiveEntry();
                        } else if (Files.isRegularFile(path)) {
                            var entry = new TarArchiveEntry(path.toFile(), name);
                            entry.setSize(Files.size(path));
                            tar.putArchiveEntry(entry);
                            Files.copy(path, tar);
                            tar.closeArchiveEntry();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to add " + path + " to archive", e);
                    }
                });
            }
        } else if (Files.isRegularFile(source)) {
            var entry = new TarArchiveEntry(source.toFile(), entryName);
            entry.setSize(Files.size(source));
            tar.putArchiveEntry(entry);
            Files.copy(source, tar);
            tar.closeArchiveEntry();
        }
    }
}
