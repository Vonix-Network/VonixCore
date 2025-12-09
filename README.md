# VonixCore

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.18.2%20|%201.20.x%20|%201.21.x-brightgreen" alt="Minecraft Versions">
  <img src="https://img.shields.io/badge/Platforms-NeoForge%20|%20Paper%20|%20Bukkit-orange" alt="Platforms">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License">
</p>

**VonixCore** is a comprehensive, all-in-one essentials mod/plugin that brings the beloved features of EssentialsX, LuckPerms, CoreProtect, and more into a single, highly optimized package. Available for **NeoForge**, **Paper**, and **Bukkit** servers, VonixCore provides powerful functionality without the complexity of managing multiple mods/plugins.

---

## 📦 Available Versions

| Platform | Minecraft Versions | Java | Status |
|----------|-------------------|------|--------|
| **NeoForge** | 1.20.2 - 1.21.x | Java 21 | ✅ Release |
| **Paper** | 1.18.2 - 1.21.x | Java 17+ | ✅ Release |
| **Bukkit/Spigot** | 1.18.2 - 1.21.x | Java 17+ | ✅ Release |

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
Complete authentication for offline/cracked servers with API integration.
- Secure Login/Register with session tokens
- API Integration for centralized account management
- Auto-authentication with configurable timeout

### 🛡️ Protection System (CoreProtect-style)
Full block logging and rollback capabilities.
- Block break/place tracking
- Rollback/Restore commands
- Lookup tools for investigation

### 🏠 Homes & Warps
Personal homes and server-wide warps.
- Multiple homes per player (configurable)
- Server warps accessible to all

### 💰 Economy System
Full-featured economy with baltop, pay, and more.
- Persistent player balances
- Player-to-player transactions
- Leaderboards

### 📍 Teleportation Suite
Comprehensive teleportation commands:
- `/tp`, `/tphere`, `/tpall`, `/tppos`
- `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`
- `/rtp` - Random teleport with safe location finding
- `/back` - Return to previous location

### 👤 Player Utilities
- `/nick`, `/seen`, `/whois`, `/ping`
- `/near`, `/getpos`, `/playtime`, `/list`
- `/hat`, `/more`, `/repair`, `/clear`

### 🌍 World Commands
- Weather: `/weather`, `/sun`, `/rain`, `/storm`
- Time: `/time`, `/day`, `/night`
- Player: `/fly`, `/god`, `/heal`, `/feed`

### 💬 Messaging & Chat
- Private messages: `/msg`, `/r`, `/ignore`
- MiniMessage formatting support
- Color codes and gradients

### 📱 Discord Integration
- Chat relay between Discord and Minecraft
- Account linking
- Event notifications (join/leave, deaths, achievements)

### 📊 XP Sync
- Automatic batch sync of player XP and playtime
- Multi-server support with retry logic
- Configurable intervals

---

## 📚 Documentation

Detailed documentation is available in the [`documentation/`](documentation/) folder:

| Module | Description |
|--------|-------------|
| [Configuration Guide](documentation/configuration.md) | Overview of all config files |
| [Commands Reference](documentation/commands.md) | Complete command list |
| [Permissions](documentation/permissions.md) | Permission nodes |
| [Protection System](documentation/protection.md) | Block logging and rollback |
| [Economy](documentation/economy.md) | Economy setup |
| [Discord Integration](documentation/discord.md) | Discord bot configuration |
| [Authentication](documentation/authentication.md) | Auth system for offline servers |
| [XP Sync](documentation/xpsync.md) | External API synchronization |

---

## 🚀 Installation

### NeoForge
1. Download `VonixCore-NeoForge-x.x.x.jar` from Releases
2. Place in your server's `mods/` folder
3. Start server to generate configs
4. Configure in `config/vonixcore-*.toml`

### Paper / Bukkit
1. Download `VonixCore-Paper-x.x.x.jar` or `VonixCore-Bukkit-x.x.x.jar`
2. Place in your server's `plugins/` folder
3. Start server to generate configs
4. Configure in `plugins/VonixCore/config.yml`

---

## 🔧 Building from Source

### Quick Build (All Platforms)

**Windows (PowerShell):**
```powershell
.\build-all.ps1
```

**Linux/macOS:**
```bash
./build-all.sh
```

Built JARs will be in the `BuildOutput/` folder.

### Manual Build

```bash
# NeoForge
cd VonixCore-NeoForge-Universal
./gradlew build

# Paper
cd VonixCore-Paper-Universal
./gradlew shadowJar

# Bukkit
cd VonixCore-Bukkit-Universal
./gradlew shadowJar
```

---

## 📋 Requirements

| Platform | Requirements |
|----------|--------------|
| NeoForge | Minecraft 1.21.1, NeoForge 21.1+, Java 21 |
| Paper | Paper 1.18.2 - 1.21.x, Java 17+ |
| Bukkit | Spigot/Bukkit 1.18.2 - 1.21.x, Java 17+ |

---

## 💡 Inspired By

| Project | Authors | Contribution |
|---------|---------|--------------|
| **[EssentialsX](https://github.com/EssentialsX/Essentials)** | EssentialsX Team | Core utilities, homes, warps, economy |
| **[LuckPerms](https://github.com/LuckPerms/LuckPerms)** | Luck (lucko) | Permission system design |
| **[CoreProtect](https://github.com/PlayPro/CoreProtect)** | Intelli (Mbtg) | Block logging and rollback |
| **[DiscordSRV](https://github.com/DiscordSRV/DiscordSRV)** | Scarsz | Discord integration concepts |

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📞 Support

- **Discord**: [Vonix Network Discord](https://discord.gg/vonix)
- **Issues**: [GitHub Issues](https://github.com/Vonix-Network/VonixCore/issues)
- **Wiki**: [Documentation](documentation/)

---

<p align="center">
  Made with ❤️ by the Vonix Network Team
</p>
