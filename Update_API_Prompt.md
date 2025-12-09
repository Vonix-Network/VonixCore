# VonixCore XP Sync API Integration Guide

This document provides the specification for integrating the VonixCore XP Sync system with your existing website API.

## Overview

The VonixCore mod/plugin syncs player XP, playtime, and optional stats to a central API endpoint at configurable intervals. The sync is performed in **batch mode** for all online players (no single-player syncs).

## API Endpoint Requirements

### Endpoint
```
POST /api/v1/xp-sync
```
Or configure a custom path - the mod uses the `api_url` config value.

### Authentication
- **Header**: `Authorization: Bearer <API_KEY>`
- The API key is configured per-server in the mod's config file.

### Request Format

```json
{
  "serverName": "survival",
  "players": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "username": "PlayerOne",
      "level": 42,
      "totalExperience": 12345,
      "playtimeSeconds": 86400,
      "currentHealth": 20.0
    },
    {
      "uuid": "6ba7b810-9dad-11d1-80b4-00c04fd430c8", 
      "username": "PlayerTwo",
      "level": 15,
      "totalExperience": 1234,
      "playtimeSeconds": 3600
    }
  ]
}
```

### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serverName` | string | ✓ | Server identifier (e.g., "survival", "creative") |
| `players` | array | ✓ | Array of player data objects |
| `players[].uuid` | string | ✓ | Player's UUID (format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`) |
| `players[].username` | string | ✓ | Player's current username |
| `players[].level` | integer | ✓ | Player's XP level (0+) |
| `players[].totalExperience` | integer | ✓ | Total accumulated XP points |
| `players[].playtimeSeconds` | integer | ○ | Total playtime in seconds (if `trackPlaytime` enabled) |
| `players[].currentHealth` | float | ○ | Current health points (if `trackHealth` enabled) |

### Expected Response

**Success (200 OK)**:
```json
{
  "success": true,
  "syncedCount": 15,
  "message": "Successfully synced 15 players"
}
```

**Authentication Error (401)**:
```json
{
  "success": false,
  "error": "Invalid or missing API key"
}
```

**Forbidden (403)**:
```json
{
  "success": false,
  "error": "Server not registered or API key lacks permission"
}
```

**Server Error (500+)**:
```json
{
  "success": false,
  "error": "Internal server error description"
}
```

## Implementation Notes

### Retry Behavior
The mod will retry failed requests up to **3 times** with a 2-second delay between attempts for:
- Server errors (5xx status codes)
- Connection timeouts
- Connection failures

It will **NOT** retry for:
- Authentication errors (401, 403)
- Client errors (4xx)
- DNS resolution failures

### Batch Processing
- Requests may contain 1-100+ players per batch
- On server startup, all players from world data files are synced
- Regular interval syncs only include online players
- On shutdown, a final sync is performed

### Headers Sent
```
Content-Type: application/json; charset=UTF-8
Accept: application/json
Authorization: Bearer <api_key>
User-Agent: VonixCore-XPSync/1.0.0  (or VonixCore-XPSync-Paper, VonixCore-XPSync-Bukkit)
```

## Database Schema Suggestion

```sql
CREATE TABLE player_stats (
  id SERIAL PRIMARY KEY,
  uuid VARCHAR(36) NOT NULL,
  username VARCHAR(16) NOT NULL,
  server_name VARCHAR(50) NOT NULL,
  level INTEGER DEFAULT 0,
  total_experience INTEGER DEFAULT 0,
  playtime_seconds INTEGER DEFAULT 0,
  current_health FLOAT DEFAULT 20.0,
  last_synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  UNIQUE(uuid, server_name)
);

CREATE INDEX idx_player_stats_uuid ON player_stats(uuid);
CREATE INDEX idx_player_stats_server ON player_stats(server_name);
```

## Example API Implementation (Next.js/Node.js)

```typescript
// app/api/v1/xp-sync/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { db } from '@/lib/db';

export async function POST(req: NextRequest) {
  // Verify API key
  const authHeader = req.headers.get('Authorization');
  if (!authHeader?.startsWith('Bearer ')) {
    return NextResponse.json({ success: false, error: 'Missing authorization' }, { status: 401 });
  }
  
  const apiKey = authHeader.substring(7);
  const server = await db.query.servers.findFirst({
    where: eq(servers.apiKey, apiKey)
  });
  
  if (!server) {
    return NextResponse.json({ success: false, error: 'Invalid API key' }, { status: 403 });
  }

  try {
    const { serverName, players } = await req.json();
    
    if (!Array.isArray(players)) {
      return NextResponse.json({ success: false, error: 'Invalid payload' }, { status: 400 });
    }

    let syncedCount = 0;
    for (const player of players) {
      await db.insert(playerStats)
        .values({
          uuid: player.uuid,
          username: player.username,
          serverName: serverName,
          level: player.level,
          totalExperience: player.totalExperience,
          playtimeSeconds: player.playtimeSeconds || 0,
          currentHealth: player.currentHealth || 20.0,
          lastSyncedAt: new Date()
        })
        .onConflictDoUpdate({
          target: [playerStats.uuid, playerStats.serverName],
          set: {
            username: player.username,
            level: player.level,
            totalExperience: player.totalExperience,
            playtimeSeconds: player.playtimeSeconds || 0,
            currentHealth: player.currentHealth || 20.0,
            lastSyncedAt: new Date()
          }
        });
      syncedCount++;
    }

    return NextResponse.json({ 
      success: true, 
      syncedCount,
      message: `Successfully synced ${syncedCount} players`
    });

  } catch (error) {
    console.error('[XP Sync API] Error:', error);
    return NextResponse.json({ success: false, error: 'Internal server error' }, { status: 500 });
  }
}
```

## Configuration Reference

In the mod/plugin configuration:

```toml
# vonixcore-xpsync.toml (NeoForge/Forge)
[general]
enabled = true
api_url = "https://vonix.network/api/v1/xp-sync"
api_key = "YOUR_API_KEY_HERE"
server_name = "survival"
sync_interval = 300  # seconds

# Optional tracking
track_playtime = true
track_health = false
verbose_logging = false
```

```yaml
# config.yml (Paper/Bukkit)
xpsync:
  enabled: true
  api_endpoint: "https://vonix.network/api/v1/xp-sync"
  api_key: "YOUR_API_KEY_HERE"
  server_name: "survival"
  sync_interval: 300
  track_playtime: true
  track_health: false
  verbose_logging: false
```

## Security Recommendations

1. **API Key Rotation**: Implement API key regeneration functionality
2. **Rate Limiting**: 100 requests/minute per API key
3. **IP Whitelisting**: Optional - restrict to known server IPs
4. **HTTPS Only**: Always use TLS encryption
5. **Audit Logging**: Log all sync requests with timestamps

## Testing

Use curl to test your endpoint:

```bash
curl -X POST https://your-domain.com/api/v1/xp-sync \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_api_key_here" \
  -d '{
    "serverName": "test",
    "players": [{
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "username": "TestPlayer",
      "level": 10,
      "totalExperience": 1000,
      "playtimeSeconds": 3600
    }]
  }'
```

Expected response:
```json
{"success":true,"syncedCount":1,"message":"Successfully synced 1 players"}
```
