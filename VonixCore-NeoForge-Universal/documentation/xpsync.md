# VonixCore XP Sync

Synchronize player XP, playtime, and statistics with an external API. Designed for the Vonix Network website but compatible with any REST API.

---

## 📋 Overview

XP Sync provides:
- **Automatic synchronization** at configurable intervals
- **Startup/shutdown sync** for all player data
- **Real-time sync** on player join/leave
- **Playtime tracking** and optional health/position data
- **Multi-server support** with server identifiers

---

## ⚙️ Configuration

**File:** `vonixcore-xpsync.toml`

```toml
[xpsync]
# Enable XP synchronization
enabled = false

[api]
# API endpoint for syncing data
endpoint = "https://yoursite.com/api/minecraft/sync/xp"

# API key for authentication (KEEP SECRET!)
api_key = "YOUR_API_KEY_HERE"

# Server name for multi-server identification
server_name = "Server-1"

# Sync interval in seconds (default: 5 minutes)
sync_interval = 300

[data]
# Track and sync playtime
track_playtime = true

# Track and sync current health
track_health = true

# Track and sync hunger level
track_hunger = false

# Track and sync position (privacy implications!)
track_position = false

[advanced]
# Enable verbose debug logging
verbose_logging = false

# Connection timeout in milliseconds
connection_timeout = 10000

# Maximum retry attempts on failure
max_retries = 3
```

---

## 🔄 Sync Behavior

### When Data is Synced

1. **Server Startup**: Syncs ALL players from world data
2. **Regular Interval**: Syncs online players (default: every 5 minutes)
3. **Player Join**: Syncs individual player data
4. **Player Leave**: Syncs individual player data
5. **Server Shutdown**: Syncs ALL players from world data

### What Gets Synced

| Data | Config Option | Default |
|------|---------------|---------|
| UUID | Always | ✅ |
| Username | Always | ✅ |
| XP Level | Always | ✅ |
| Total XP | Always | ✅ |
| Playtime | `track_playtime` | ✅ |
| Health | `track_health` | ✅ |
| Hunger | `track_hunger` | ❌ |
| Position | `track_position` | ❌ |

---

## 📊 API Specification

### Endpoint

```
POST {api_endpoint}
Authorization: Bearer {api_key}
Content-Type: application/json
```

### Request Payload

```json
{
  "serverName": "Survival-1",
  "players": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "username": "Steve",
      "level": 30,
      "totalExperience": 1395,
      "playtimeSeconds": 36000,
      "currentHealth": 20.0
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "username": "Alex",
      "level": 15,
      "totalExperience": 315,
      "playtimeSeconds": 18000,
      "currentHealth": 18.5
    }
  ]
}
```

### Expected Response

```json
{
  "success": true,
  "syncedCount": 2,
  "message": "Sync completed successfully"
}
```

### Error Response

```json
{
  "success": false,
  "error": "Invalid API key"
}
```

---

## 🔒 Authentication

XP Sync uses Bearer token authentication:

```
Authorization: Bearer YOUR_API_KEY_HERE
```

### Security Recommendations

1. **Use HTTPS**: Always use encrypted endpoints
2. **Rotate Keys**: Periodically rotate API keys
3. **Restrict IPs**: Whitelist server IPs if possible
4. **Monitor Usage**: Watch for unusual sync patterns

---

## 📈 Multi-Server Setup

For networks with multiple servers:

**Server 1 Config:**
```toml
server_name = "Survival-1"
```

**Server 2 Config:**
```toml
server_name = "Creative"
```

**Server 3 Config:**
```toml
server_name = "SkyBlock"
```

The API can then aggregate data across all servers.

---

## 🔍 Debugging

Enable verbose logging for troubleshooting:

```toml
[advanced]
verbose_logging = true
```

### Log Messages

**Successful sync:**
```
[XPSync] Successfully synced 15 players
```

**Connection error:**
```
[XPSync] Cannot connect to API: https://yoursite.com/api/...
```

**Authentication error:**
```
[XPSync] Authentication failed! Check your API key.
```

**Timeout:**
```
[XPSync] Request timed out: https://yoursite.com/api/...
```

---

## 💾 Offline Player Data

XP Sync reads offline player data from:
- `world/playerdata/*.dat` files
- `world/stats/*.json` files

This allows syncing ALL players on startup/shutdown, not just online players.

---

## 🛠️ Troubleshooting

### Sync Not Working

1. **Check enabled**: Ensure `enabled = true`
2. **Check endpoint**: Verify URL is correct and reachable
3. **Check API key**: Ensure key is valid
4. **Check logs**: Enable verbose logging

### Players Not Appearing

1. **Check sync interval**: Data syncs periodically
2. **Force sync**: Player join/leave triggers immediate sync
3. **Check UUID format**: Must be valid UUID string

### High Latency

1. **Increase timeout**: Raise `connection_timeout`
2. **Reduce frequency**: Increase `sync_interval`
3. **Optimize API**: Check API server performance

---

## 📊 Sample Website Integration

Example PHP endpoint to receive XP data:

```php
<?php
// Verify API key
$headers = getallheaders();
$authHeader = $headers['Authorization'] ?? '';
if ($authHeader !== 'Bearer YOUR_API_KEY') {
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Invalid API key']);
    exit;
}

// Parse request
$data = json_decode(file_get_contents('php://input'), true);
$serverName = $data['serverName'];
$players = $data['players'];

// Store in database
$pdo = new PDO('mysql:host=localhost;dbname=website', 'user', 'pass');
foreach ($players as $player) {
    $stmt = $pdo->prepare('
        INSERT INTO player_stats (uuid, username, server, level, xp, playtime)
        VALUES (?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE 
        username = VALUES(username),
        level = VALUES(level),
        xp = VALUES(xp),
        playtime = VALUES(playtime),
        updated_at = NOW()
    ');
    $stmt->execute([
        $player['uuid'],
        $player['username'],
        $serverName,
        $player['level'],
        $player['totalExperience'],
        $player['playtimeSeconds'] ?? 0
    ]);
}

echo json_encode([
    'success' => true,
    'syncedCount' => count($players)
]);
```

---

## 🔗 Related Documentation

- [Configuration Guide](configuration.md)
- [Commands Reference](commands.md)
