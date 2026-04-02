package it.pruefert.backupmodule.model;

public record BackupResponse(
        String jobId,
        String status
) {}
