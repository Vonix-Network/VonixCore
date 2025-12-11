# Graves System

VonixCore features a robust Graves system that secures your items upon death.

## Features

- **Death Chests**: A chest (grave) is spawned at your death location containing your items.
- **XP Storage**: A configurable percentage of your XP is stored in the grave.
- **Protection**: Graves are protected from other players for a configurable time (default 5 minutes).
- **Auto-Cleanup**: Expired graves are automatically removed after a set time (default 1 hour).
- **Cross-Platform**: Works identically on NeoForge, Forge, and Bukkit.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/graves` | List your active graves with coordinates | - |
| `/graves list` | Alias for `/graves` | - |
| `/graves status` | View system status and total grave count | `vonixcore.admin` |

## Configuration

Located in `vonixcore-graves.yml`.

```yaml
graves:
  enabled: true
  
  # Time in seconds before grave expires
  expiration-time: 3600
  
  # Time in seconds grave is protected from others
  protection-time: 300
  
  # Percentage of XP to keep (0.0 to 1.0)
  xp-retention: 0.8
  
  # Maximum active graves per player
  max-graves-per-player: 5
```

## How It Works

1. **On Death**: A grave block is placed at the nearest safe location to your death point. All compatible items and XP are transferred to it.
2. **Retrieval**: Return to the location and right-click the grave (or break it) to retrieve your items and XP.
   - If you die again, a new grave is created (up to the max limit).
   - If you exceed the limit, the oldest grave is removed to make room.
3. **Expiration**: If not looted within the expiration time, the grave is removed and items are lost.
