# Changelog

All notable changes to VonixCore will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-12-09

### 🎉 Initial Release

VonixCore is a comprehensive, all-in-one essentials mod/plugin for Minecraft servers.

### Platforms
- **NeoForge** - Minecraft 1.20.2 - 1.21.x (Java 21)
- **Paper** - Minecraft 1.18.2 - 1.21.x (Java 17+)
- **Bukkit/Spigot** - Minecraft 1.18.2 - 1.21.x (Java 17+)

### Features

#### 🗄️ Database System
- Unified single-database storage for all data
- Multiple backend support: SQLite, MySQL, PostgreSQL, Turso, Supabase
- HikariCP connection pooling for optimal performance
- Automatic table creation and migration

#### 🔐 Authentication
- Complete auth system for offline/cracked servers
- Secure password hashing with session tokens
- API integration for centralized account management
- Configurable session timeouts

#### 🛡️ Protection (CoreProtect-style)
- Block break/place logging
- Container access tracking
- Rollback and restore commands
- Player lookup and investigation tools

#### 🏠 Homes & Warps
- Multiple homes per player (configurable limits)
- Server-wide warp points
- Cooldowns and per-permission home limits

#### 💰 Economy
- Full economy system with persistent balances
- `/pay`, `/balance`, `/baltop` commands
- Admin commands for giving/taking/setting money
- Configurable currency symbol and names

#### 📍 Teleportation
- `/tp`, `/tphere`, `/tpall`, `/tppos`
- `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`
- `/rtp` - Random teleport with safe location finding
- `/back` - Return to previous location

#### 👤 Player Utilities
- `/nick` - Nicknames with color code support
- `/seen`, `/whois`, `/ping`, `/near`, `/getpos`
- `/playtime`, `/list`, `/afk`

#### 🎒 Item Commands
- `/hat`, `/more`, `/repair`, `/clear`

#### 🌍 World Commands
- Weather: `/sun`, `/rain`, `/storm`
- Time: `/day`, `/night`, `/time set`
- Player state: `/fly`, `/god`, `/heal`, `/feed`

#### 💬 Chat & Messaging
- Private messaging: `/msg`, `/r`, `/ignore`
- MiniMessage formatting support
- Legacy `&` color codes and hex colors

#### 📱 Discord Integration
- Chat relay between Discord and Minecraft
- Account linking system
- Join/leave/death announcements
- Webhook and bot support via Javacord

#### 📊 XP Sync
- Batch synchronization of player XP and playtime
- 3-retry logic with exponential backoff
- Multi-server support with server identifiers
- Configurable sync intervals

#### 🔑 Permission System
- Built-in permission system with groups
- LuckPerms auto-detection and integration
- Prefix/suffix support
- Group inheritance and weights

### Technical
- Optimized for minimal CPU and memory usage
- Proper thread management and shutdown handling
- HikariCP for database connection pooling
- Async operations for network calls

---

## Future Releases

Stay tuned for updates! Follow us on:
- [GitHub](https://github.com/Vonix-Network/VonixCore)
- [Discord](https://discord.gg/vonix)
