# VonixCore

<div align="center">

**The Ultimate Cross-Platform Minecraft Server Core**

[![NeoForge](https://img.shields.io/badge/NeoForge-1.21.x-blue)](https://neoforged.net/)
[![Forge](https://img.shields.io/badge/Forge-1.20.1-orange)](https://minecraftforge.net/)
[![Bukkit](https://img.shields.io/badge/Bukkit%2FPaper-1.18%2B-green)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

[Features](#-features) • [Installation](#-installation) • [Documentation](#-documentation) • [Support](#-support)

</div>

---

## 🎮 Platform Support

| Platform | Version | Build |
|----------|---------|-------|
| **NeoForge** | 1.21.x | `VonixCore-NeoForge-*.jar` |
| **Forge** | 1.20.1 | `VonixCore-Forge-1.20.1-*.jar` |
| **Bukkit/Paper** | 1.18+ | `VonixCore-Bukkit-*.jar` |
| **Paper** | 1.18+ | `VonixCore-Paper-*.jar` |

## ✨ Features

### 🏠 Essentials
- **Homes** - Personal teleport points
- **Warps** - Server-wide locations
- **TPA** - Teleport requests
- **Kits** - Configurable item packages

### 💰 Economy
- **Virtual Currency** - Balance, pay, transactions
- **Chest Shops** - Player-to-player trading
- **Sign Shops** - Quick buy/sell signs
- **Server Shop** - Admin-managed GUI shop
- **Player Market** - Auction house
- **Jobs** - Earn money by playing

### 🛡️ Protection
- **Block Logging** - CoreProtect-style tracking
- **Rollback/Restore** - Undo griefing
- **Inspector Mode** - Click to see history
- **Extended Logging** - Chat, commands, signs

### ⚰️ Graves
- Items stored safely on death
- Configurable XP retention
- Protection timer
- Auto-cleanup

### 🏗️ Claims
- Land protection
- Trust system
- Shop bypass integration
- Explosion protection

### 🔧 Utilities
- 30+ utility commands
- Discord webhooks
- XP synchronization
- Authentication system

## 📦 Installation

### Bukkit/Spigot/Paper
```bash
# Download VonixCore-Bukkit-*.jar
# Place in plugins/ folder
# Restart server
```

### NeoForge/Forge
```bash
# Download appropriate VonixCore-*.jar
# Place in mods/ folder
# Restart server
```

## 📖 Documentation

Full documentation is available in the [`documentation/`](documentation/) folder:

| Document | Description |
|----------|-------------|
| [**Commands**](documentation/commands.md) | Complete command reference |
| [**Configuration**](documentation/configuration.md) | Config file guide |
| [**Permissions**](documentation/permissions.md) | Permission nodes |
| [**Economy**](documentation/economy.md) | Economy & shops |
| [**Jobs**](documentation/jobs.md) | Earn money by playing |
| [**Protection**](documentation/protection.md) | Block logging & rollback |
| [**Claims**](documentation/claims.md) | Land protection |
| [**Graves**](documentation/graves.md) | Death protection |
| [**Authentication**](documentation/authentication.md) | Login system |
| [**Discord**](documentation/discord.md) | Discord integration |
| [**XP Sync**](documentation/xpsync.md) | Experience sync |

## ⚙️ Configuration

VonixCore uses modular config files:

| File | Purpose |
|------|---------|
| `vonixcore-database` | Database connection |
| `vonixcore-essentials` | Homes, warps, TPA, kits |
| `vonixcore-protection` | Block logging settings |
| `vonixcore-claims` | Land claims |
| `vonixcore-graves` | Death protection |
| `vonixcore-discord` | Discord webhooks |
| `vonixcore-xpsync` | XP synchronization |

**Bukkit**: `.yml` files in `plugins/VonixCore/`  
**Forge/NeoForge**: `.toml` files in `config/`

## 🗄️ Database

Supports multiple databases:
- **SQLite** (default) - Zero configuration
- **MySQL/MariaDB** - Multi-server setups
- **PostgreSQL** - Enterprise deployments
- **Turso/LibSQL** - Edge database

## 🔗 Support

- **GitHub**: [github.com/Vonix-Network/VonixCore](https://github.com/Vonix-Network/VonixCore)
- **Discord**: [discord.gg/vonix](https://discord.gg/vonix)
- **Website**: [vonix.network](https://vonix.network)

## 📄 License

MIT License - See [LICENSE](LICENSE) for details.

---

<div align="center">
Made with ❤️ by <a href="https://vonix.network">Vonix Network</a>
</div>
