# Aethera Async Backups Module

Asynchronous backup service for [Aethera](https://github.com/jaameypr/aethera-next) game server management.

## What it does

When installed as an Aethera module, this service takes over backup creation from the lightweight built-in method. Instead of blocking the Next.js process while packing tar.gz archives, backup jobs are sent to this Java Spring Boot service which processes them asynchronously in the background.

## Features

- **Async backup creation** — non-blocking, runs in a thread pool
- **Callback notifications** — notifies Aethera when a backup completes or fails
- **Paperview integration** — optionally uploads backups to Paperview for sharing
- **Health monitoring** — Spring Boot Actuator health endpoint

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/backups/create` | Submit a backup job (returns 202 Accepted) |
| GET | `/api/backups/jobs/{jobId}` | Check job status |
| GET | `/api/backups/jobs` | List all jobs |
| GET | `/actuator/health` | Health check |

### Create Backup Request

```json
{
  "serverId": "MongoDB ObjectId of the server",
  "serverIdentifier": "server directory name",
  "serverName": "display name",
  "components": ["world", "config", "mods", "plugins", "datapacks"],
  "callbackUrl": "http://aethera-app:3000/api/backups/callback",
  "actorId": "userId who triggered the backup"
}
```

### Callback Payload (sent to Aethera)

```json
{
  "jobId": "uuid",
  "serverId": "MongoDB ObjectId",
  "status": "completed",
  "filename": "2026-04-02T14-30-45-123Z-MyServer.tar.gz",
  "path": "/data/backups/{serverId}/...",
  "size": 123456789,
  "shareUrl": "https://paperview.example.com/pages/shares/...",
  "shareId": "paperview-share-id"
}
```

## Configuration

| Env Variable | Description | Default |
|---|---|---|
| `AETHERA_CALLBACK_URL` | URL Aethera listens on for job callbacks | `http://aethera-app:3000/api/backups/callback` |
| `AETHERA_API_KEY` | API key for authenticating requests from Aethera | (empty = no auth) |
| `AETHERA_DATA_DIR` | Path to server data directories | `/data/servers` |
| `AETHERA_BACKUP_DIR` | Path to backup storage | `/data/backups` |
| `PAPERVIEW_URL` | Paperview internal URL (optional) | (empty = disabled) |
| `PAPERVIEW_API_KEY` | Paperview API key (optional) | (empty = disabled) |

## Development

```bash
mvn spring-boot:run
```

## Docker

```bash
docker build -t aethera-mod-async-backups:1.0.0 .
docker run -p 8080:8080 \
  -v /path/to/servers:/data/servers \
  -v /path/to/backups:/data/backups \
  aethera-mod-async-backups:1.0.0
```
