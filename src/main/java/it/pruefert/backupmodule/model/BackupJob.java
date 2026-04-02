package it.pruefert.backupmodule.model;

import java.time.Instant;
import java.util.List;

public class BackupJob {

    public enum Status { PENDING, IN_PROGRESS, COMPLETED, FAILED }

    private final String jobId;
    private final String serverId;
    private final String serverIdentifier;
    private final String serverName;
    private final String backupId;
    private final List<String> components;
    private final String callbackUrl;
    private final String actorId;
    private final Instant createdAt;

    private volatile Status status;
    private volatile String filename;
    private volatile String path;
    private volatile long size;
    private volatile String shareUrl;
    private volatile String shareId;
    private volatile String error;

    public BackupJob(String jobId, BackupRequest request) {
        this.jobId = jobId;
        this.serverId = request.serverId();
        this.serverIdentifier = request.serverIdentifier();
        this.serverName = request.serverName() != null ? request.serverName() : request.serverIdentifier();
        this.backupId = request.backupId();
        this.components = request.components();
        this.callbackUrl = request.callbackUrl();
        this.actorId = request.actorId();
        this.createdAt = Instant.now();
        this.status = Status.PENDING;
    }

    public String getJobId() { return jobId; }
    public String getServerId() { return serverId; }
    public String getServerIdentifier() { return serverIdentifier; }
    public String getServerName() { return serverName; }
    public String getBackupId() { return backupId; }
    public List<String> getComponents() { return components; }
    public String getCallbackUrl() { return callbackUrl; }
    public String getActorId() { return actorId; }
    public Instant getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getShareUrl() { return shareUrl; }
    public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }
    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
