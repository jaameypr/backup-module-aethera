package it.pruefert.backupmodule.controller;

import it.pruefert.backupmodule.model.BackupJob;
import it.pruefert.backupmodule.model.BackupRequest;
import it.pruefert.backupmodule.model.BackupResponse;
import it.pruefert.backupmodule.service.BackupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/backups")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Accept a new backup job. Returns immediately with 202 Accepted.
     * The backup runs in the background and calls back to Aethera when done.
     */
    @PostMapping("/create")
    public ResponseEntity<BackupResponse> createBackup(@RequestBody BackupRequest request) {
        BackupJob job = backupService.submitJob(request);
        backupService.executeBackup(job);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new BackupResponse(job.getJobId(), "accepted"));
    }

    /** Get status of a specific backup job. */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> getJob(@PathVariable String jobId) {
        return backupService.getJob(jobId)
                .map(job -> ResponseEntity.ok(Map.of(
                        "jobId", job.getJobId(),
                        "status", job.getStatus().name().toLowerCase(),
                        "serverId", job.getServerId(),
                        "serverIdentifier", job.getServerIdentifier(),
                        "createdAt", job.getCreatedAt().toString(),
                        "filename", job.getFilename() != null ? job.getFilename() : "",
                        "size", job.getSize(),
                        "shareUrl", job.getShareUrl() != null ? job.getShareUrl() : "",
                        "error", job.getError() != null ? job.getError() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /** List all jobs (for debugging). */
    @GetMapping("/jobs")
    public Collection<Map<String, Object>> listJobs() {
        return backupService.getAllJobs().stream()
                .map(job -> Map.<String, Object>of(
                        "jobId", job.getJobId(),
                        "status", job.getStatus().name().toLowerCase(),
                        "serverId", job.getServerId(),
                        "serverIdentifier", job.getServerIdentifier(),
                        "createdAt", job.getCreatedAt().toString()
                ))
                .toList();
    }
}
