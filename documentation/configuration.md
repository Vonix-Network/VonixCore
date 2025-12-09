# VonixCore Configuration Guide

This guide covers all configuration files and their options. VonixCore uses a modular configuration system where each feature has its own `.toml` file in your server's `config/` directory.

---

## 📁 Configuration Files Overview

| File | Description |
|------|-------------|
| `vonixcore-database.toml` | Database connection and storage |
| `vonixcore-protection.toml` | Block logging and rollback |
| `vonixcore-essentials.toml` | Homes, warps, economy, kits |
| `vonixcore-discord.toml` | Discord integration |
| `vonixcore-xpsync.toml` | XP synchronization |

---

## 🗄️ Database Configuration

**File:** `vonixcore-database.toml`

VonixCore stores **all data in a single database** and supports multiple database backends:

| Type | Description | Best For |
|------|-------------|----------|
| `sqlite` | Local file (default) | Single servers, simplicity |
| `mysql` | MySQL/MariaDB server | Multi-server networks |
| `postgresql` | PostgreSQL server | Advanced features, scale |
| `turso` | Turso LibSQL edge DB | Global edge, low latency |
| `supabase` | Supabase PostgreSQL | Serverless, web integration |

### Database Type Selection

```toml
[database]
# Options: sqlite, mysql, postgresql, turso, supabase
type = "sqlite"
```

---

### SQLite Configuration (Default)

Best for single-server setups. All data stored in one file.

```toml
[sqlite]
# Database file (stored in world/vonixcore/)
file = "vonixcore.db"
```

**Location:** `<world>/vonixcore/vonixcore.db`

---

### MySQL/MariaDB Configuration

For networks or servers requiring a dedicated database server.

```toml
[mysql]
host = "localhost"
port = 3306
database = "vonixcore"
username = "root"
password = "your_password"
ssl = false
```

**Required:** Create the database first:
```sql
CREATE DATABASE vonixcore;
```

---

### PostgreSQL Configuration

For servers requiring PostgreSQL's advanced features.

```toml
[postgresql]
host = "localhost"
port = 5432
database = "vonixcore"
username = "postgres"
password = "your_password"
ssl = false
```

---

### Turso Configuration (LibSQL Edge Database)

[Turso](https://turso.tech) provides globally distributed SQLite-compatible databases.

```toml
[turso]
# Your Turso database URL
url = "libsql://your-database.turso.io"

# Auth token from Turso dashboard (KEEP SECRET!)
auth_token = "your_auth_token"
```

**Setup:**
1. Create account at [turso.tech](https://turso.tech)
2. Create a database: `turso db create vonixcore`
3. Get URL: `turso db show vonixcore --url`
4. Get token: `turso db tokens create vonixcore`

---

### Supabase Configuration

[Supabase](https://supabase.com) provides hosted PostgreSQL with a free tier.

```toml
[supabase]
# Your project's database host
host = "db.xxxxxxxxxxxx.supabase.co"

# Port (5432 for direct, 6543 for pooled)
port = 5432

# Database name (usually 'postgres')
database = "postgres"

# Database password from Supabase dashboard (KEEP SECRET!)
password = "your_database_password"
```

**Setup:**
1. Create project at [supabase.com](https://supabase.com)
2. Go to **Settings → Database**
3. Copy host, port, and password

---

### Connection Pool Settings

Applies to all database types:

```toml
[pool]
# Maximum connections (5-10 small, 10-20 large)
max_connections = 10

# Connection timeout in milliseconds
timeout_ms = 5000
```

---

### Performance Tuning

```toml
[performance]
# Records to batch before writing
batch_size = 500

# Delay between batch writes (ms)
batch_delay_ms = 500

# Auto-purge data older than X days (0 = never)
purge_days = 30
```

---

## 🛡️ Protection Configuration

**File:** `vonixcore-protection.toml`

See [Protection System](protection.md) for detailed usage.

```toml
[protection]
enabled = true
log_retention_days = 30
max_lookup_results = 1000
log_containers = true
log_entity_kills = false
batch_size = 100
```

---

## 🏠 Essentials Configuration

**File:** `vonixcore-essentials.toml`

```toml
[essentials]
enabled = true

[homes]
enabled = true
default_max_homes = 3
cooldown = 5

[warps]
enabled = true
cooldown = 3

[economy]
enabled = true
starting_balance = 100.0
currency_symbol = "$"
currency_name = "Dollar"
currency_name_plural = "Dollars"

[tpa]
enabled = true
request_timeout = 60
cooldown = 30
```

---

## 📱 Discord Configuration

**File:** `vonixcore-discord.toml`

See [Discord Integration](discord.md) for setup.

```toml
[discord]
enabled = false
bot_token = "YOUR_BOT_TOKEN"
chat_channel_id = "000000000000000000"

[discord.messages]
minecraft_to_discord = "**{player}**: {message}"
discord_to_minecraft = "&9[Discord] &b{user}&r: {message}"

[discord.events]
announce_join = true
announce_leave = true
announce_death = true

[discord.account_linking]
enabled = true
link_code_expiry = 300
```

---

## 📊 XP Sync Configuration

**File:** `vonixcore-xpsync.toml`

See [XP Sync](xpsync.md) for API details.

```toml
[xpsync]
enabled = false

[api]
endpoint = "https://yoursite.com/api/minecraft/sync/xp"
api_key = "YOUR_API_KEY"
server_name = "Server-1"
sync_interval = 300

[data]
track_playtime = true
track_health = true
track_hunger = false
track_position = false

[advanced]
verbose_logging = false
connection_timeout = 10000
max_retries = 3
```

---

## 💡 Tips

1. **Always restart** after changing config files
2. **Backup database** before switching database types
3. **Test locally** before production changes
4. **Secure credentials** - never share config files with passwords

---

## 🔗 Related Documentation

- [Commands Reference](commands.md)
- [Permissions](permissions.md)
- [Protection System](protection.md)
- [Discord Integration](discord.md)
