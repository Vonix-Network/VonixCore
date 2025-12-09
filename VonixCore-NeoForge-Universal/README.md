# VonixCore

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/NeoForge-21.1.x-orange" alt="NeoForge Version">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License">
</p>

**VonixCore** is a comprehensive, all-in-one essentials mod for NeoForge 1.21.1 that brings the beloved features of EssentialsX, LuckPerms, CoreProtect, and more into a single, highly optimized server-side mod. Designed for performance and user-friendliness, VonixCore is perfect for server owners who want powerful functionality without the complexity of managing multiple mods.

---

## ✨ Features

### 🗄️ Flexible Database Storage
All data stored in a **single database** with support for multiple backends:
- **SQLite** - Single-file storage (default, perfect for single servers)
- **MySQL/MariaDB** - Traditional database server
- **PostgreSQL** - Advanced PostgreSQL features
- **Turso** - LibSQL edge database (global low-latency)
- **Supabase** - Serverless PostgreSQL with web dashboard

### 🔐 Authentication System
Complete authentication for offline/cracked servers with API integration for the Vonix Network ecosystem.
- **Secure Login/Register** - Password-based authentication with session tokens
- **API Integration** - Connects to Vonix Network for centralized account management
- **Auto-authentication** - Configurable session timeout and remember-me functionality

### 🛡️ Protection System (CoreProtect-style)
Full block logging and rollback capabilities to protect your server from griefers.
- **Block Logging** - Tracks all block breaks, placements, and interactions
- **Rollback/Restore** - Undo griefing damage with precise rollback commands
- **Lookup Tools** - Inspect who did what, when, and where

### 🏠 Homes & Warps
Let players set personal homes and server-wide warps for easy navigation.
- **Multiple Homes** - Configurable per-player home limits with permissions
- **Server Warps** - Admin-managed warp points accessible to all

### 💰 Economy System
Full-featured economy with baltop, pay, and configurable starting balance.
- **Player Balances** - SQLite-backed persistent economy
- **Transactions** - Pay other players, check balance, view leaderboards

### 📍 Teleportation Commands
Comprehensive teleportation suite rivaling EssentialsX.
- `/tp <player>` - Teleport to a player
- `/tphere <player>` - Summon a player to you
- `/tpall` - Teleport all players to you
- `/tppos <x> <y> <z>` - Teleport to coordinates
- `/rtp` - Random teleport with safe location finding
- `/setspawn` - Set world spawn point

### 👤 Player Utilities
Essential quality-of-life commands for players and admins.
- `/nick <name>` - Set nickname with color code support
- `/seen <player>` - Check last online status
- `/whois <player>` - Detailed player information
- `/ping` - Check your latency
- `/near [radius]` - Find nearby players
- `/getpos` - Display current coordinates
- `/playtime` - Show total playtime
- `/list` - Enhanced player list

### 💬 Messaging System
Private messaging and social features.
- `/msg <player> <message>` - Send private messages
- `/r <message>` - Reply to last message
- `/ignore <player>` - Block messages from a player

### 🎒 Item Commands
Handy item manipulation commands.
- `/hat` - Wear held item as helmet
- `/more` - Fill item stack to max
- `/repair` - Repair held item
- `/clear [player]` - Clear inventory

### 🌍 World Commands
Control weather, time, and world mechanics.
- `/weather clear|rain|storm` - Set weather
- `/time set day|night|noon|midnight|<ticks>` - Set time
- `/sun`, `/rain`, `/storm` - Quick weather shortcuts
- `/day`, `/night` - Quick time shortcuts
- `/lightning [player]` - Strike lightning
- `/ext [player]` - Extinguish player fire
- `/afk [message]` - Toggle AFK status

### 🔑 Permission System
Built-in permission system with LuckPerms auto-detection and fallback.
- **Groups & Inheritance** - Create groups with parent inheritance
- **Prefixes & Suffixes** - Custom chat formatting per group/player
- **Wildcard Permissions** - Support for `*` wildcards
- **LuckPerms Detection** - Automatically uses LuckPerms if installed
- **Database Storage** - Persistent SQLite storage

### 💬 Chat Formatting
MiniMessage-style chat formatting with rich text support.
- **Color Codes** - Legacy `&a` and hex `&#RRGGBB` support
- **MiniMessage Tags** - `<red>`, `<bold>`, `<gradient:#FF0000:#00FF00>`
- **Rainbow Text** - `<rainbow>` gradient effects
- **Prefix/Suffix** - Automatic integration with permission system

### 📱 Discord Integration
Bridge your Minecraft server with Discord.
- **Chat Relay** - Sync messages between Discord and Minecraft
- **Account Linking** - Link Minecraft accounts to Discord
- **Event Notifications** - Player join/leave, deaths, achievements

### 📊 XP Sync
Synchronize player XP data with external APIs.
- **Automatic Sync** - Regular intervals with configurable timing
- **Playtime Tracking** - Track and sync player statistics
- **Multi-Server Support** - Identify data by server name

---

## 📚 Documentation

Detailed documentation for each module is available in the [`documentation/`](documentation/) folder:

| Module | Description |
|--------|-------------|
| [Configuration Guide](documentation/configuration.md) | Overview of all config files and options |
| [Commands Reference](documentation/commands.md) | Complete list of all commands |
| [Permissions](documentation/permissions.md) | Permission system and nodes |
| [Protection System](documentation/protection.md) | Block logging and rollback |
| [Economy](documentation/economy.md) | Economy system setup |
| [Discord Integration](documentation/discord.md) | Discord bot configuration |
| [Authentication](documentation/authentication.md) | Auth system for offline servers |
| [XP Sync](documentation/xpsync.md) | External API synchronization |

---

## 🚀 Installation

1. **Download** the latest release from the [Releases](https://github.com/Vonix-Network/VonixCore-NeoForge/releases) page
2. **Place** the `.jar` file in your server's `mods/` folder
3. **Start** your server to generate config files
4. **Configure** the modules you want in `config/vonixcore-*.toml`
5. **Restart** and enjoy!

### Requirements
- Minecraft 1.21.1
- NeoForge 21.1.x or higher
- Java 21 or higher

---

## ⚙️ Configuration

VonixCore uses a modular configuration system with separate files for each feature:

| File | Purpose |
|------|---------|
| `vonixcore-database.toml` | Database connection settings |
| `vonixcore-protection.toml` | Block logging and rollback |
| `vonixcore-essentials.toml` | Homes, warps, economy, kits |
| `vonixcore-discord.toml` | Discord integration |
| `vonixcore-xpsync.toml` | XP synchronization |

Each module can be enabled/disabled independently. See the [Configuration Guide](documentation/configuration.md) for detailed options.

---

## 🔧 Building from Source

```bash
git clone https://github.com/Vonix-Network/VonixCore-NeoForge.git
cd vonixcore-template-1.21.1
./gradlew build
```

The compiled JAR will be in `build/libs/`.

---

## 💡 Inspired By

VonixCore is proudly inspired by the incredible work of these projects and their developers:

| Project | Authors | Contribution |
|---------|---------|--------------|
| **[EssentialsX](https://github.com/EssentialsX/Essentials)** | EssentialsX Team | Core utility commands, homes, warps, economy |
| **[LuckPerms](https://github.com/LuckPerms/LuckPerms)** | Luck (lucko) | Permission system design and architecture |
| **[CoreProtect](https://github.com/PlayPro/CoreProtect)** | Intelli (Mbtg) | Block logging and rollback mechanics |
| **[DiscordSRV](https://github.com/DiscordSRV/DiscordSRV)** | Scarsz | Discord integration concepts |

A huge thank you to these developers for their open-source contributions to the Minecraft community. VonixCore aims to bring these beloved features to the NeoForge ecosystem while maintaining the spirit of the original projects.

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📞 Support

- **Discord**: [Vonix Network Discord](https://discord.gg/vonix)
- **Issues**: [GitHub Issues](https://github.com/Vonix-Network/VonixCore-NeoForge/issues)
- **Wiki**: [Documentation](documentation/)

---

<p align="center">
  Made with ❤️ by the Vonix Network Team
</p>
