package it.pruefert.backupmodule.model;

import java.util.List;

public record BackupRequest(
        String serverId,
        String serverIdentifier,
        String serverName,
        List<String> components,
        String callbackUrl,
        String backupId,
        String actorId
) {
    public BackupRequest {
        if (serverId == null || serverId.isBlank())
            throw new IllegalArgumentException("serverId is required");
        if (serverIdentifier == null || serverIdentifier.isBlank())
            throw new IllegalArgumentException("serverIdentifier is required");
        if (components == null || components.isEmpty())
            throw new IllegalArgumentException("components must not be empty");
    }
}
