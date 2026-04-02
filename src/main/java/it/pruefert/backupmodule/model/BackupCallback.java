package it.pruefert.backupmodule.model;

public record BackupCallback(
        String jobId,
        String serverId,
        String status,
        String filename,
        String path,
        long size,
        String shareUrl,
        String shareId,
        String error
) {
    public static BackupCallback success(String jobId, String serverId, String filename,
                                         String path, long size, String shareUrl, String shareId) {
        return new BackupCallback(jobId, serverId, "completed", filename, path, size, shareUrl, shareId, null);
    }

    public static BackupCallback failure(String jobId, String serverId, String error) {
        return new BackupCallback(jobId, serverId, "failed", null, null, 0, null, null, error);
    }
}
