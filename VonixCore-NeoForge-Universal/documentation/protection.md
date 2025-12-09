# VonixCore Protection System

The Protection System provides CoreProtect-style block logging and rollback capabilities to protect your server from griefing.

---

## 📋 Overview

The protection system logs:
- Block breaks and placements
- Container interactions (chests, furnaces, etc.)
- Entity kills (optional)
- Player interactions

All data is stored in the database and can be queried or rolled back.

---

## ⚙️ Configuration

**File:** `vonixcore-protection.toml`

```toml
[protection]
# Enable the protection module
enabled = true

# Days to keep log data (0 = forever)
log_retention_days = 30

# Maximum results per lookup
max_lookup_results = 1000

# Log container (chest) interactions
log_containers = true

# Log entity kills
log_entity_kills = false

# Batch size for async database writes
batch_size = 100
```

---

## 🔍 Lookup Commands

### Basic Lookup

```
/co lookup <parameters>
```

View block history at a location or by player.

### Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `u:<user>` | Filter by username | `u:Steve` |
| `t:<time>` | Time range | `t:1d` (1 day), `t:3h` (3 hours) |
| `r:<radius>` | Radius around you | `r:10` |
| `a:<action>` | Action type | `a:break`, `a:place`, `a:container` |
| `b:<block>` | Block type | `b:diamond_ore` |

### Examples

```bash
# Who broke blocks near me in the last hour?
/co lookup r:5 t:1h a:break

# What did Steve do in the last day?
/co lookup u:Steve t:1d

# Who placed TNT in the last week?
/co lookup t:7d a:place b:tnt
```

---

## ⏪ Rollback Commands

### Rollback

```
/co rollback <parameters>
```

Undo changes matching the specified parameters.

### Examples

```bash
# Rollback Steve's changes in the last 12 hours within 20 blocks
/co rollback u:Steve t:12h r:20

# Rollback all TNT explosions in the last day
/co rollback t:1d a:explosion r:50

# Rollback block breaks only
/co rollback u:Griefer t:6h r:30 a:break
```

### Restore

```
/co restore <parameters>
```

Redo a rollback (restore changes that were rolled back).

```bash
/co restore u:Steve t:12h r:20
```

---

## 🔧 Inspector Tool

### Toggle Inspector

```
/co inspect
```

or

```
/co i
```

Toggles the block inspector. When enabled, left-clicking or right-clicking a block shows its history.

### Inspector Output

```
[VonixCore] Block history for Stone at (100, 64, -200):
- Steve placed 2 hours ago
- Alex broke 6 hours ago
- Steve placed 1 day ago
```

---

## 📊 Time Format

The time parameter supports various formats:

| Format | Meaning |
|--------|---------|
| `1s` | 1 second |
| `30m` | 30 minutes |
| `6h` | 6 hours |
| `1d` | 1 day |
| `2w` | 2 weeks |
| `1mo` | 1 month |

You can combine them:
- `1d12h` = 1 day and 12 hours
- `2w3d` = 2 weeks and 3 days

---

## 🎯 Action Types

| Action | Description |
|--------|-------------|
| `break` | Block breaks |
| `place` | Block placements |
| `container` | Chest/container access |
| `click` | Block interactions |
| `kill` | Entity kills |
| `explosion` | Explosion damage |

---

## 🔑 Permissions

| Permission | Description |
|------------|-------------|
| `vonixcore.protection.lookup` | Use lookup commands |
| `vonixcore.protection.rollback` | Use rollback commands |
| `vonixcore.protection.restore` | Use restore commands |
| `vonixcore.protection.inspect` | Use inspector tool |
| `vonixcore.protection.*` | All protection permissions |

---

## 💡 Best Practices

1. **Regular Purge**: Set `log_retention_days` to prevent database bloat
2. **Backup First**: Always backup before large rollbacks
3. **Test Radius**: Start with small radius for complex rollbacks
4. **Use Inspector First**: Check block history before rollback

---

## 🔗 Related Documentation

- [Commands Reference](commands.md)
- [Configuration Guide](configuration.md)
