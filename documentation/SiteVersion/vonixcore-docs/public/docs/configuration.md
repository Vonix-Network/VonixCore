# VonixCore Configuration Guide

Comprehensive guide to configuring VonixCore for your server.

## Configuration Files

VonixCore uses modular configuration files for easy management:

| File | Purpose |
|------|---------|
| `vonixcore-database.yml` | Database connection settings |
| `vonixcore-essentials.yml` | Homes, warps, TPA, kits |
| `vonixcore-protection.yml` | Block logging and rollback |
| `vonixcore-discord.yml` | Discord webhook integration |
| `vonixcore-xpsync.yml` | XP synchronization |
| `vonixcore-graves.yml` | Graves system |
| `vonixcore-shops.yml` | Shop configuration |

> **Platform Note:**
> - **NeoForge/Forge**: Config files use `.toml` format and are located in `config/vonixcore/`
> - **Bukkit**: Config files use `.yml` format and are located in `plugins/VonixCore/`

---

## Database Configuration

**File:** `vonixcore-database.yml`

### SQLite (Default)

```yaml
database:
  type: sqlite
  file: vonixcore.db
```

### MySQL/MariaDB

```yaml
database:
  type: mysql
  host: localhost
  port: 3306
  database: vonixcore
  username: root
  password: your_password
  pool:
    minimum: 2
    maximum: 10
```

### PostgreSQL

```yaml
database:
  type: postgresql
  host: localhost
  port: 5432
  database: vonixcore
  username: postgres
  password: your_password
  ssl: false
```

---

## Essentials Configuration

**File:** `vonixcore-essentials.yml`

```yaml
# Homes
homes:
  enabled: true
  max-homes: 3              # Default max homes per player
  max-homes-vip: 10         # Max homes for VIP players
  teleport-delay: 3         # Seconds before teleporting

# Warps
warps:
  enabled: true
  teleport-delay: 3

# TPA
tpa:
  enabled: true
  timeout: 60               # Seconds before request expires
  cooldown: 30              # Seconds between requests

# Kits
kits:
  enabled: true
  # Kits are defined in the database
```

---

## Protection Configuration

**File:** `vonixcore-protection.yml`

```yaml
protection:
  enabled: true
  
  # Logging settings
  logging:
    blocks: true            # Log block changes
    containers: true        # Log chest transactions
    entities: true          # Log entity kills
    chat: false             # Log chat messages
    commands: false         # Log commands
    signs: true             # Log sign text
  
  # Data retention
  retention:
    days: 30                # Days to keep data
    auto-purge: true        # Auto-delete old data
  
  # Rollback settings
  rollback:
    max-blocks: 50000       # Max blocks per rollback
    require-confirm: true   # Require confirmation for large rollbacks
```

---

## Discord Configuration

**File:** `vonixcore-discord.yml`

```yaml
discord:
  enabled: false
  webhook-url: "https://discord.com/api/webhooks/..."
  
  # Events to send
  events:
    player-join: true
    player-leave: true
    player-death: false
    player-chat: false
    server-start: true
    server-stop: true
  
  # Message format
  format:
    join: "**{player}** joined the server"
    leave: "**{player}** left the server"
    death: "**{player}** {death_message}"
```

---

## XPSync Configuration

**File:** `vonixcore-xpsync.yml`

```yaml
xpsync:
  enabled: true
  sync-interval: 300        # Seconds between syncs
  sync-on-join: true        # Sync when player joins
  sync-on-leave: true       # Sync when player leaves
```

---

## Graves Configuration

**File:** `vonixcore-graves.yml`

```yaml
graves:
  enabled: true
  
  # Timing
  expiration-time: 3600     # Seconds before grave expires
  protection-time: 300      # Seconds of owner-only access
  
  # XP
  xp-retention: 0.8         # Percentage of XP stored (0.0 - 1.0)
  
  # Limits
  max-graves-per-player: 5  # Max active graves per player
  
  # Hologram (requires HologramLib)
  hologram:
    enabled: false
    lines:
      - "&c☠ &f{player}'s Grave &c☠"
      - "&7Items: &e{items}"
      - "&7XP: &e{xp}"
      - "&7Expires: &e{time}"
```

---

## Shops Configuration

**File:** `vonixcore-shops.yml`

```yaml
shops:
  enabled: true
  
  # GUI Shop
  gui-shop:
    enabled: true
    title: "&6Server Shop"
    rows: 6
  
  # Chest Shops
  chest-shops:
    enabled: true
    max-per-player: 10
    tax-rate: 0.05          # 5% tax on sales
  
  # Player Market
  market:
    enabled: true
    max-listings: 20
    listing-fee: 10         # Fee to list an item
    listing-duration: 7     # Days before listing expires
    tax-rate: 0.1           # 10% tax on sales
  
  # Jobs
  jobs:
    enabled: true
    max-jobs: 3             # Max jobs per player
```

---

## Authentication Configuration

**File:** `vonixcore-auth.yml` (if using auth)

```yaml
auth:
  enabled: true
  
  # Behavior
  require-authentication: true
  freeze-unauthenticated: true
  warn-of-auth: false
  login-timeout: 60         # Seconds before kick
  
  # API
  api:
    base-url: "https://vonix.network/api"
    api-key: "YOUR_API_KEY"
    timeout: 5000           # Milliseconds
  
  # Messages
  messages:
    login-required: "&c&l⚠ &cYou must authenticate to play."
    auth-success: "&a&l✓ &7Successfully authenticated as &e{username}"
    login-failed: "&c&l✗ &7Login failed: &c{error}"
```

---

## Permission Integration

### Bukkit

VonixCore works with any permission plugin (LuckPerms, PermissionsEx, etc.)

### Forge/NeoForge

Uses Minecraft's built-in permission levels:
- Level 0: All players
- Level 2: Operators (admins)
- Level 4: Server console

---

## Tips

### Performance Optimization

```yaml
# vonixcore-database.yml
database:
  pool:
    minimum: 5              # Increase for busy servers
    maximum: 20

# vonixcore-protection.yml
protection:
  logging:
    chat: false             # Disable if not needed
    commands: false         # Disable if not needed
  retention:
    days: 14                # Reduce for less storage
```

### Multi-Server Setup

For BungeeCord/Velocity networks:
1. Use MySQL/PostgreSQL for shared database
2. Enable XPSync with same database
3. Configure unique server names

---

## Troubleshooting

### Database Connection Issues

1. Check credentials
2. Ensure database exists
3. Check firewall rules
4. Verify pool settings

### Missing Commands

1. Check plugin.yml for registration
2. Verify permissions
3. Reload plugin: `/vonix reload`

### High Memory Usage

1. Reduce protection retention days
2. Lower max rollback blocks
3. Disable unnecessary logging
