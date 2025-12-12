# VonixCore

A comprehensive, cross-platform Minecraft server core plugin/mod providing essential features for servers.

## 🎮 Supported Platforms

| Platform | Minecraft Version | Status |
|----------|------------------|--------|
| **NeoForge** | 1.20.2 - 1.21.x | ✅ Fully Supported |
| **Forge** | 1.20.1 | ✅ Fully Supported |
| **Bukkit/Spigot/Paper** | 1.18+ | ✅ Fully Supported |

### Platform Differences

| Feature | NeoForge | Forge 1.20.1 | Bukkit |
|---------|----------|--------------|--------|
| **Config Format** | `.toml` | `.toml` | `.yml` |
| **Config Location** | `config/vonixcore/` | `config/vonixcore/` | `plugins/VonixCore/` |
| **Permissions** | OP levels (0-4) | OP levels (0-4) | LuckPerms/Vault |
| **GUI Shops** | ✅ Full | ✅ Full | ✅ Full |
| **Chest Shops** | ✅ Full | ✅ Full | ✅ Full |
| **Jobs System** | ✅ Full | ✅ Full | ✅ Full |
| **XP Sync** | ✅ All players | ✅ All players | ✅ Online only |
| **Offline Data** | NBT + Stats | NBT + Stats | Stats only |
| **Display Entities** | ✅ Holograms | ✅ Holograms | Armor Stands |

## ✨ Features

### Core Systems
- **Homes System** - Set, teleport to, and manage personal homes
- **Warps System** - Server-wide warp points with GUI
- **TPA System** - Teleport requests between players
- **Kits System** - Configurable item kits with cooldowns

### Economy
- **Virtual Currency** - Balance, pay commands
- **GUI Shop** - Server-side item shop with GUI
- **Chest Shops** - Player-to-player trading via chests
- **Sign Shops** - Quick buy/sell via signs
- **Player Market** - Auction house for player listings
- **Jobs System** - Earn money by mining, farming, fishing, etc.

### Protection (CoreProtect-style)
- **Block Logging** - Track all block changes
- **Entity Logging** - Track entity kills and interactions
- **Container Logging** - Track chest/inventory transactions
- **Rollback/Restore** - Undo/redo block changes by player/time/radius
- **Inspector Mode** - Click blocks to see their history
- **Extended Logging** - Chat, commands, signs

### Graves System
- **Death Protection** - Items stored in graves on death
- **XP Retention** - Configurable XP percentage saved
- **Protection Timer** - Prevent others from looting immediately
- **Auto-Cleanup** - Expired graves removed automatically

### Authentication
- **Login/Register** - Account-based authentication
- **Freeze System** - Unauthenticated players can't move/interact
- **Vonix Network API** - Integration with external auth service

### Utility Commands
- **Teleportation** - `/tp`, `/tphere`, `/tppos`, `/tpall`, `/rtp`
- **Player Info** - `/nick`, `/seen`, `/whois`, `/ping`, `/near`, `/playtime`
- **Messaging** - `/msg`, `/r`, `/ignore`
- **Admin Tools** - `/heal`, `/feed`, `/fly`, `/god`, `/speed`, `/clear`, `/repair`, `/invsee`

### Integrations
- **Discord Webhooks** - Send events to Discord
- **XP Sync** - Synchronize player XP with database

## 📦 Installation

### Bukkit/Spigot/Paper
1. Download `VonixCore-Bukkit-Universal.jar`
2. Place in your server's `plugins/` folder
3. Start the server to generate config files
4. Configure in `plugins/VonixCore/` directory

### NeoForge/Forge
1. Download the appropriate mod JAR
2. Place in your server's `mods/` folder
3. Start the server to generate config files
4. Configure in `config/vonixcore/` directory

## ⚙️ Configuration

VonixCore uses modular configuration files:

| File | Purpose |
|------|---------|
| `vonixcore-database.yml` | Database connection (SQLite/MySQL/PostgreSQL) |
| `vonixcore-essentials.yml` | Homes, warps, TPA, kits settings |
| `vonixcore-protection.yml` | Block logging and rollback settings |
| `vonixcore-discord.yml` | Discord webhook integration |
| `vonixcore-xpsync.yml` | XP synchronization settings |
| `vonixcore-graves.yml` | Graves system settings |
| `vonixcore-shops.yml` | Shop system configuration |

## 📋 Commands

### Homes
| Command | Description | Permission |
|---------|-------------|------------|
| `/sethome [name]` | Set a home | `vonixcore.homes` |
| `/home [name]` | Teleport to home | `vonixcore.homes` |
| `/delhome [name]` | Delete a home | `vonixcore.homes` |
| `/homes` | List your homes | `vonixcore.homes` |

### Warps
| Command | Description | Permission |
|---------|-------------|------------|
| `/warp <name>` | Teleport to warp | `vonixcore.warps` |
| `/setwarp <name>` | Create a warp | `vonixcore.admin` |
| `/delwarp <name>` | Delete a warp | `vonixcore.admin` |
| `/warps` | List all warps | `vonixcore.warps` |

### Teleportation
| Command | Description | Permission |
|---------|-------------|------------|
| `/tpa <player>` | Request teleport | - |
| `/tpaccept` | Accept request | - |
| `/tpdeny` | Deny request | - |
| `/tp <player>` | Teleport to player | `vonixcore.tp` |
| `/tphere <player>` | Teleport player to you | `vonixcore.tphere` |
| `/rtp` | Random teleport | `vonixcore.rtp` |

### Economy
| Command | Description | Permission |
|---------|-------------|------------|
| `/balance` | Check balance | - |
| `/pay <player> <amount>` | Pay player | - |
| `/shop` | Open server shop | `vonixcore.shop` |
| `/cshop` | Chest shop commands | `vonixcore.shops.create` |
| `/market` | Player market | `vonixcore.market` |
| `/jobs` | Jobs system | `vonixcore.jobs` |

### Protection
| Command | Description | Permission |
|---------|-------------|------------|
| `/co inspect` | Toggle inspector mode | `vonixcore.protection.inspect` |
| `/co lookup` | Search block history | `vonixcore.protection.lookup` |
| `/co rollback` | Rollback changes | `vonixcore.protection.rollback` |
| `/co restore` | Restore changes | `vonixcore.protection.restore` |
| `/co undo` | Undo last rollback | `vonixcore.protection.rollback` |

### Utility
| Command | Description | Permission |
|---------|-------------|------------|
| `/heal [player]` | Heal player | `vonixcore.heal` |
| `/feed [player]` | Feed player | `vonixcore.feed` |
| `/fly [player]` | Toggle fly | `vonixcore.fly` |
| `/god [player]` | Toggle god mode | `vonixcore.god` |
| `/nick [name]` | Set nickname | `vonixcore.nick` |
| `/msg <player> <msg>` | Private message | - |
| `/r <msg>` | Reply to message | - |

### Graves
| Command | Description | Permission |
|---------|-------------|------------|
| `/graves` | List your graves | - |
| `/graves list` | List your graves | - |
| `/graves status` | System status | `vonixcore.admin` |

## 🔧 Database Support

VonixCore supports multiple databases:

- **SQLite** (default) - No setup required
- **MySQL/MariaDB** - For multi-server setups
- **PostgreSQL** - Enterprise-grade option

## 🛡️ Permissions

See `plugin.yml` (Bukkit) or in-game for full permission list.

Default permission groups:
- `vonixcore.*` - All permissions
- `vonixcore.admin` - Admin commands
- `vonixcore.mod` - Moderator commands

## 📊 API

VonixCore provides an API for other plugins/mods:

```java
// Get economy balance
VonixCore.getInstance().getEconomy().getBalance(uuid);

// Log protection event
VonixCore.getInstance().getProtection().logBlock(player, block, action);
```

## 🔗 Links

- **GitHub**: [github.com/Vonix-Network/VonixCore](https://github.com/Vonix-Network/VonixCore)
- **Discord**: [discord.gg/vonix](https://discord.gg/vonix)
- **Documentation**: [docs.vonix.network](https://docs.vonix.network)

## 📄 License

MIT License - See LICENSE file for details.

---

Made with ❤️ by [Vonix Network](https://vonix.network)
