# VonixCore Commands Reference

Complete reference of all commands available in VonixCore. Commands are organized by category.

---

## 📍 Teleportation Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/tp <player>` | `vonixcore.tp` | Teleport to a player |
| `/tp <player> <target>` | `vonixcore.tp.others` | Teleport a player to another player |
| `/tphere <player>` | `vonixcore.tphere` | Teleport a player to you |
| `/tpall` | `vonixcore.tpall` | Teleport all players to you |
| `/tppos <x> <y> <z>` | `vonixcore.tppos` | Teleport to coordinates |
| `/rtp` | `vonixcore.rtp` | Random teleport to a safe location |
| `/setspawn` | `vonixcore.setspawn` | Set the world spawn point |

### TPA (Teleport Request) Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/tpa <player>` | `vonixcore.tpa` | Request to teleport to a player |
| `/tpahere <player>` | `vonixcore.tpahere` | Request a player to teleport to you |
| `/tpaccept` | `vonixcore.tpa` | Accept a teleport request |
| `/tpdeny` | `vonixcore.tpa` | Deny a teleport request |

---

## 🏠 Home Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/home [name]` | `vonixcore.home` | Teleport to your home |
| `/sethome [name]` | `vonixcore.sethome` | Set a home at your location |
| `/delhome <name>` | `vonixcore.delhome` | Delete a home |
| `/homes` | `vonixcore.home` | List all your homes |

### Home Limits
- Default players get 3 homes
- Configure with permission: `vonixcore.sethome.multiple.<count>`
- Example: `vonixcore.sethome.multiple.10` allows 10 homes

---

## 🚀 Warp Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/warp <name>` | `vonixcore.warp` | Teleport to a warp |
| `/setwarp <name>` | `vonixcore.setwarp` | Create a new warp |
| `/delwarp <name>` | `vonixcore.delwarp` | Delete a warp |
| `/warps` | `vonixcore.warp` | List all warps |

---

## 💰 Economy Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/balance` or `/bal` | `vonixcore.balance` | Check your balance |
| `/balance <player>` | `vonixcore.balance.others` | Check another player's balance |
| `/pay <player> <amount>` | `vonixcore.pay` | Pay another player |
| `/baltop [page]` | `vonixcore.baltop` | View richest players |
| `/eco give <player> <amount>` | `vonixcore.eco` | Give money to a player |
| `/eco take <player> <amount>` | `vonixcore.eco` | Take money from a player |
| `/eco set <player> <amount>` | `vonixcore.eco` | Set a player's balance |

---

## 👤 Player Utility Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/nick <name>` | `vonixcore.nick` | Set your nickname (supports color codes) |
| `/nick` | `vonixcore.nick` | Clear your nickname |
| `/seen <player>` | `vonixcore.seen` | Check when a player was last online |
| `/whois <player>` | `vonixcore.whois` | View detailed player information |
| `/ping` | `vonixcore.ping` | Check your latency |
| `/near [radius]` | `vonixcore.near` | Find nearby players (default: 100 blocks) |
| `/getpos` | `vonixcore.getpos` | Display your current coordinates |
| `/playtime` | `vonixcore.playtime` | Show your total playtime |
| `/list` | `vonixcore.list` | Enhanced player list |
| `/suicide` | `vonixcore.suicide` | Kill yourself |

### Nickname Color Codes
Use `&` for color codes in nicknames:
- `&a` = Green, `&b` = Aqua, `&c` = Red, etc.
- `&#RRGGBB` = Hex color (e.g., `&#FF5500`)

---

## 💬 Messaging Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/msg <player> <message>` | `vonixcore.msg` | Send a private message |
| `/tell <player> <message>` | `vonixcore.msg` | Alias for /msg |
| `/r <message>` | `vonixcore.msg` | Reply to last message |
| `/reply <message>` | `vonixcore.msg` | Alias for /r |
| `/ignore <player>` | `vonixcore.ignore` | Toggle ignoring a player |

---

## 🎒 Item Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/hat` | `vonixcore.hat` | Wear held item as helmet |
| `/more` | `vonixcore.more` | Fill held item stack to max |
| `/repair` | `vonixcore.repair` | Repair held item |
| `/clear` | `vonixcore.clear` | Clear your inventory |
| `/clear <player>` | `vonixcore.clear.others` | Clear another player's inventory |

---

## 🌍 World Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/weather clear` | `vonixcore.weather` | Set weather to clear |
| `/weather rain` | `vonixcore.weather` | Set weather to rain |
| `/weather storm` | `vonixcore.weather` | Set weather to thunderstorm |
| `/sun` | `vonixcore.weather` | Shortcut for clear weather |
| `/rain` | `vonixcore.weather` | Shortcut for rain |
| `/storm` | `vonixcore.weather` | Shortcut for storm |
| `/time set <value>` | `vonixcore.time` | Set time (day/night/noon/midnight/ticks) |
| `/day` | `vonixcore.time` | Shortcut for daytime |
| `/night` | `vonixcore.time` | Shortcut for nighttime |
| `/lightning [player]` | `vonixcore.lightning` | Strike lightning at player or look position |
| `/smite [player]` | `vonixcore.lightning` | Alias for /lightning |
| `/ext [player]` | `vonixcore.ext` | Extinguish fire on player |
| `/afk [message]` | `vonixcore.afk` | Toggle AFK status with optional message |

---

## 🖥️ Server Management Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/broadcast <message>` | `vonixcore.broadcast` | Broadcast a message to all players |
| `/bc <message>` | `vonixcore.broadcast` | Alias for /broadcast |
| `/gc` | `vonixcore.gc` | Show server memory and thread stats |
| `/lag` | `vonixcore.lag` | Check server performance |
| `/invsee <player>` | `vonixcore.invsee` | View another player's inventory |
| `/enderchest [player]` | `vonixcore.enderchest` | Open ender chest (yours or another's) |
| `/workbench` | `vonixcore.workbench` | Open a virtual crafting table |
| `/anvil` | `vonixcore.anvil` | Open a virtual anvil |

---

## 🔑 Permission Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/perm user <player> info` | `vonixcore.perm` | View player permission info |
| `/perm user <player> group set <group>` | `vonixcore.perm` | Set player's primary group |
| `/perm user <player> group add <group>` | `vonixcore.perm` | Add player to a group |
| `/perm user <player> group remove <group>` | `vonixcore.perm` | Remove player from a group |
| `/perm user <player> permission set <perm> <true/false>` | `vonixcore.perm` | Set a permission |
| `/perm user <player> permission unset <perm>` | `vonixcore.perm` | Remove a permission |
| `/perm user <player> permission check <perm>` | `vonixcore.perm` | Check if player has permission |
| `/perm user <player> meta setprefix <prefix>` | `vonixcore.perm` | Set player's prefix |
| `/perm user <player> meta setsuffix <suffix>` | `vonixcore.perm` | Set player's suffix |
| `/perm group <group> info` | `vonixcore.perm` | View group info |
| `/perm group <group> create` | `vonixcore.perm` | Create a new group |
| `/perm group <group> delete` | `vonixcore.perm` | Delete a group |
| `/perm group <group> permission set <perm> <true/false>` | `vonixcore.perm` | Set group permission |
| `/perm group <group> meta setprefix <prefix>` | `vonixcore.perm` | Set group prefix |
| `/perm group <group> meta setsuffix <suffix>` | `vonixcore.perm` | Set group suffix |
| `/perm group <group> meta setweight <weight>` | `vonixcore.perm` | Set group weight (priority) |
| `/perm group <group> parent set <parent>` | `vonixcore.perm` | Set group's parent |
| `/perm listgroups` | `vonixcore.perm` | List all groups |
| `/lp ...` | `vonixcore.perm` | Alias for /perm |

See [Permissions](permissions.md) for detailed permission system documentation.

---

## ⚙️ VonixCore Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/vonixcore` | `vonixcore.admin` | Show VonixCore help |
| `/vonixcore reload` | `vonixcore.admin` | Reload all configurations |
| `/vonixcore reload <module>` | `vonixcore.admin` | Reload specific module config |
| `/vonixcore version` | `vonixcore.admin` | Show VonixCore version |
| `/vonixcore status` | `vonixcore.admin` | Show enabled/disabled modules |

### Reload Modules
Available modules for `/vonixcore reload <module>`:
- `all` - Reload all configurations
- `database` - Database connection settings
- `protection` - Block logging settings
- `essentials` - Homes, warps, economy, kits settings
- `discord` - Discord integration settings
- `xpsync` - XP sync settings
- `auth` - Authentication settings

---

## 🛒 Shop Commands

### GUI Shop Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/shop` | `vonixcore.shop` | Open admin/server shop GUI |
| `/shop server` | `vonixcore.shop` | Open server shop GUI |
| `/shop player` | `vonixcore.shop` | Open player market GUI |
| `/shop player sell` | `vonixcore.shop.sell` | List held item for sale |
| `/market` | `vonixcore.shop` | Open player market GUI |

### Quick Sell Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/sell hand` | `vonixcore.sell` | Sell held item to server shop |
| `/sell all` | `vonixcore.sell` | Sell all sellable items in inventory |
| `/daily` | `vonixcore.daily` | Claim daily reward |

### Chest Shop Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/chestshop create` | `vonixcore.chestshop` | Start chest shop creation |
| `/chestshop remove` | `vonixcore.chestshop` | Remove your chest shop |
| `/chestshop cancel` | `vonixcore.chestshop` | Cancel shop creation |
| `/chestshop info` | `vonixcore.chestshop` | View chest shop information |

### Admin Shop Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/adminshop setprice <item> <buy> <sell>` | `vonixcore.adminshop` | Set server shop prices |
| `/adminshop list` | `vonixcore.adminshop` | List all server shop items |

### Sign Shops
Create sign shops by placing a sign with this format:
```
[Buy] or [Sell]
<quantity>
<item name>
$<price>
```
Example buy sign:
```
[Buy]
16
diamond
$500
```

---

## 📱 Discord Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/discord link` | `vonixcore.discord.link` | Generate a link code |
| `/discord unlink` | `vonixcore.discord.link` | Unlink your Discord account |

---

## 🔗 Related Documentation

- [Configuration Guide](configuration.md)
- [Permissions](permissions.md)
- [Protection System](protection.md)
