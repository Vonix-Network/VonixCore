# VonixCore Permission System

VonixCore includes a complete permission system that works standalone or integrates with LuckPerms. This guide covers setup, commands, and all permission nodes.

---

## 🔍 LuckPerms Detection

VonixCore automatically detects if LuckPerms is installed:

- **LuckPerms installed**: VonixCore uses LuckPerms for all permission checks, prefixes, and suffixes
- **LuckPerms not installed**: VonixCore uses its built-in permission system with SQLite storage

You don't need to configure anything - detection is automatic at server startup.

---

## 📋 Built-in Permission System

When LuckPerms is not installed, VonixCore provides a full-featured permission system:

### Features
- **Groups** with hierarchical inheritance
- **User-specific permissions** that override group permissions
- **Prefixes and Suffixes** for chat formatting
- **Weights** for group priority
- **Wildcard permissions** (`vonixcore.*`, `*`)
- **Negative permissions** (set to false to deny)

### Default Group
A `default` group is automatically created with no permissions. All new players are assigned to this group.

---

## 🔧 Permission Commands

All commands require OP level 3 or the `vonixcore.perm` permission.

### User Commands

```
/perm user <player> info
```
View player's groups, prefix, suffix, and permissions.

```
/perm user <player> group set <group>
```
Set the player's primary group.

```
/perm user <player> group add <group>
```
Add player to an additional group.

```
/perm user <player> group remove <group>
```
Remove player from a group.

```
/perm user <player> permission set <permission> <true|false>
```
Set a permission for the player. Use `false` to explicitly deny.

```
/perm user <player> permission unset <permission>
```
Remove a permission override from the player.

```
/perm user <player> permission check <permission>
```
Check if a player has a specific permission.

```
/perm user <player> meta setprefix <prefix>
```
Set the player's chat prefix. Use `&` for color codes.

```
/perm user <player> meta setsuffix <suffix>
```
Set the player's chat suffix.

```
/perm user <player> meta clearprefix
/perm user <player> meta clearsuffix
```
Clear the player's prefix or suffix.

### Group Commands

```
/perm group <group> info
```
View group information.

```
/perm group <group> create
```
Create a new permission group.

```
/perm group <group> delete
```
Delete a group. Cannot delete the default group.

```
/perm group <group> permission set <permission> <true|false>
```
Set a permission for the group.

```
/perm group <group> permission unset <permission>
```
Remove a permission from the group.

```
/perm group <group> meta setprefix <prefix>
```
Set the group's chat prefix.

```
/perm group <group> meta setsuffix <suffix>
```
Set the group's chat suffix.

```
/perm group <group> meta setweight <weight>
```
Set the group's weight (priority). Higher = more important.

```
/perm group <group> meta setdisplayname <name>
```
Set a display name for the group.

```
/perm group <group> parent set <parent>
```
Set the group's parent for inheritance.

```
/perm group <group> parent clear
```
Remove the group's parent.

```
/perm listgroups
```
List all permission groups.

### Aliases
- `/lp` - Alias for `/perm`
- `/permissions` - Alias for `/perm`

---

## 🎨 Prefix and Suffix Formatting

Prefixes and suffixes support color codes:

### Legacy Color Codes
Use `&` followed by a color code:
- `&0-9` and `&a-f` for standard colors
- `&l` = Bold, `&o` = Italic, `&n` = Underline
- `&r` = Reset

### Hex Colors
Use `&#RRGGBB` for custom colors:
- `&#FF5500` = Orange
- `&#00FF00` = Green

### Example
```
/perm group admin meta setprefix &c[Admin] 
/perm group vip meta setprefix &#FFD700[VIP] 
```

---

## 📊 Group Inheritance

Groups can inherit permissions from a parent group:

```
/perm group admin parent set default
/perm group owner parent set admin
```

This creates a hierarchy:
- `owner` inherits from `admin` inherits from `default`
- Child groups automatically have all parent permissions

### Priority (Weight)
When a player is in multiple groups, the group with the highest weight determines prefix/suffix:

```
/perm group default meta setweight 1
/perm group vip meta setweight 10
/perm group admin meta setweight 100
```

---

## 🔑 Permission Nodes

### Core Permissions

| Permission | Description |
|------------|-------------|
| `vonixcore.*` | All VonixCore permissions |
| `vonixcore.perm` | Access permission commands |

### Teleportation

| Permission | Description |
|------------|-------------|
| `vonixcore.tp` | Teleport to players |
| `vonixcore.tp.others` | Teleport other players |
| `vonixcore.tphere` | Teleport players to you |
| `vonixcore.tpall` | Teleport all players |
| `vonixcore.tppos` | Teleport to coordinates |
| `vonixcore.rtp` | Random teleport |
| `vonixcore.setspawn` | Set world spawn |
| `vonixcore.tpa` | TPA requests |
| `vonixcore.tpahere` | TPA here requests |

### Homes

| Permission | Description |
|------------|-------------|
| `vonixcore.home` | Use home commands |
| `vonixcore.sethome` | Set homes |
| `vonixcore.sethome.multiple.<n>` | Set n number of homes |
| `vonixcore.delhome` | Delete homes |

### Warps

| Permission | Description |
|------------|-------------|
| `vonixcore.warp` | Use warps |
| `vonixcore.setwarp` | Create warps |
| `vonixcore.delwarp` | Delete warps |

### Economy

| Permission | Description |
|------------|-------------|
| `vonixcore.balance` | Check balance |
| `vonixcore.balance.others` | Check others' balance |
| `vonixcore.pay` | Pay other players |
| `vonixcore.baltop` | View balance leaderboard |
| `vonixcore.eco` | Admin economy commands |

### Player Utilities

| Permission | Description |
|------------|-------------|
| `vonixcore.nick` | Set nickname |
| `vonixcore.nick.color` | Use colors in nickname |
| `vonixcore.seen` | Check last online |
| `vonixcore.whois` | View player info |
| `vonixcore.ping` | Check latency |
| `vonixcore.near` | Find nearby players |
| `vonixcore.getpos` | Get coordinates |
| `vonixcore.playtime` | View playtime |
| `vonixcore.list` | Enhanced player list |

### Messaging

| Permission | Description |
|------------|-------------|
| `vonixcore.msg` | Private messages |
| `vonixcore.ignore` | Ignore players |

### Items

| Permission | Description |
|------------|-------------|
| `vonixcore.hat` | Wear items as helmet |
| `vonixcore.more` | Fill stack |
| `vonixcore.repair` | Repair items |
| `vonixcore.clear` | Clear own inventory |
| `vonixcore.clear.others` | Clear others' inventory |

### World

| Permission | Description |
|------------|-------------|
| `vonixcore.weather` | Change weather |
| `vonixcore.time` | Change time |
| `vonixcore.lightning` | Strike lightning |
| `vonixcore.ext` | Extinguish fire |
| `vonixcore.afk` | Toggle AFK |

### Server Management

| Permission | Description |
|------------|-------------|
| `vonixcore.broadcast` | Broadcast messages |
| `vonixcore.gc` | View server stats |
| `vonixcore.lag` | View TPS |
| `vonixcore.invsee` | View inventories |
| `vonixcore.enderchest` | Open ender chests |
| `vonixcore.enderchest.others` | Open others' ender chests |
| `vonixcore.workbench` | Virtual workbench |
| `vonixcore.anvil` | Virtual anvil |

### Discord

| Permission | Description |
|------------|-------------|
| `vonixcore.discord.link` | Link Discord account |

---

## 💡 Quick Setup Example

Set up a basic permission structure:

```bash
# Create groups
/perm group default create
/perm group vip create
/perm group admin create

# Set up inheritance
/perm group vip parent set default
/perm group admin parent set vip

# Set weights
/perm group default meta setweight 1
/perm group vip meta setweight 10
/perm group admin meta setweight 100

# Set prefixes
/perm group default meta setprefix &7[Member] 
/perm group vip meta setprefix &6[VIP] 
/perm group admin meta setprefix &c[Admin] 

# Grant permissions
/perm group default permission set vonixcore.home true
/perm group default permission set vonixcore.tpa true
/perm group vip permission set vonixcore.sethome.multiple.5 true
/perm group admin permission set vonixcore.* true

# Add a player to admin
/perm user Steve group set admin
```

---

## 🔗 Related Documentation

- [Commands Reference](commands.md)
- [Configuration Guide](configuration.md)
