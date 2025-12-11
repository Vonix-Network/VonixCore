# VonixCore Command Reference

Complete list of all commands available in VonixCore.

## Table of Contents

- [Homes Commands](#homes-commands)
- [Warps Commands](#warps-commands)
- [Teleport Commands](#teleport-commands)
- [Economy Commands](#economy-commands)
- [Shop Commands](#shop-commands)
- [Jobs Commands](#jobs-commands)
- [Protection Commands](#protection-commands)
- [Graves Commands](#graves-commands)
- [Authentication Commands](#authentication-commands)
- [Utility Commands](#utility-commands)
- [Admin Commands](#admin-commands)

---

## Homes Commands

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/sethome` | Set default home | `/sethome` | `vonixcore.homes` |
| `/sethome <name>` | Set named home | `/sethome base` | `vonixcore.homes` |
| `/home` | Teleport to default home | `/home` | `vonixcore.homes` |
| `/home <name>` | Teleport to named home | `/home base` | `vonixcore.homes` |
| `/delhome` | Delete default home | `/delhome` | `vonixcore.homes` |
| `/delhome <name>` | Delete named home | `/delhome base` | `vonixcore.homes` |
| `/homes` | List all your homes | `/homes` | `vonixcore.homes` |

**Configuration:** Maximum homes per player set in `vonixcore-essentials.yml`

---

## Warps Commands

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/warp <name>` | Teleport to a warp | `/warp spawn` | `vonixcore.warps` |
| `/warps` | List all warps | `/warps` | `vonixcore.warps` |
| `/setwarp <name>` | Create a warp | `/setwarp spawn` | `vonixcore.admin` |
| `/delwarp <name>` | Delete a warp | `/delwarp spawn` | `vonixcore.admin` |

---

## Teleport Commands

### Player Teleport Requests

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/tpa <player>` | Request to teleport to player | `/tpa Steve` | - |
| `/tpaccept` | Accept teleport request | `/tpaccept` | - |
| `/tpdeny` | Deny teleport request | `/tpdeny` | - |

### Admin Teleport

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/tp <player>` | Teleport to player | `/tp Steve` | `vonixcore.tp` |
| `/tp <player> <target>` | Teleport player to target | `/tp Steve Alex` | `vonixcore.tp` |
| `/tphere <player>` | Teleport player to you | `/tphere Steve` | `vonixcore.tphere` |
| `/tppos <x> <y> <z>` | Teleport to coordinates | `/tppos 100 64 -200` | `vonixcore.tppos` |
| `/tpall` | Teleport all to you | `/tpall` | `vonixcore.tpall` |
| `/rtp` | Random teleport | `/rtp` | `vonixcore.rtp` |

### Random Teleport (RTP)

The `/rtp` command teleports players to a random safe location 500-5000 blocks from their current position.

**Safety Features:**
- **Chunk Loading**: Target chunks are loaded before location validation
- **100 Attempts**: Up to 100 random locations are tested to find a safe spot
- **Ground Stability**: Requires at least 2 solid blocks below the spawn point
- **Fall Damage Prevention**: No teleporting to spots with 4+ air blocks below
- **Hazard Avoidance**: Avoids lava, magma blocks, fire, cactus, berry bushes, wither roses, pointed dripstone
- **Suffocation Prevention**: Ensures 2 blocks of air at spawn height
- **Lava Proximity Check**: Scans a 5x4x5 area around spawn for nearby lava
- **Dimension-Aware**: Special handling for Nether (avoids bedrock) and End (avoids void)

**Dimension Behavior:**
- **Overworld**: Uses heightmap for efficient surface detection
- **Nether**: Searches Y=32-120 for air pockets in netherack
- **End**: Searches Y=50-120 to avoid void (won't teleport below Y=50)

---

## Economy Commands

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/balance` | Check your balance | `/balance` | - |
| `/bal` | Alias for balance | `/bal` | - |
| `/pay <player> <amount>` | Pay another player | `/pay Steve 100` | - |

---

### Server Shop (Admin Shop)

The admin shop has infinite inventory - items can be bought and sold without stock limits.

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/shop` | Open admin shop GUI | `/shop` | `vonixcore.shop` |
| `/adminshop setprice <item> <buy> <sell>` | Set buy/sell prices for item | `/adminshop setprice diamond 100 50` | `vonixcore.admin` |
| `/adminshop list` | List all admin shop prices | `/adminshop list` | `vonixcore.admin` |

**Admin Shop Features:**
- **Infinite Stock**: Players can buy/sell unlimited amounts
- **Buy Price**: Cost for players to purchase from shop (set to 0 to disable buying)
- **Sell Price**: Amount players receive when selling to shop (set to 0 to disable selling)
- **Item IDs**: Use Minecraft item IDs (e.g., `diamond`, `iron_ingot`, `minecraft:oak_log`)

**Examples:**
```
/adminshop setprice diamond 100 50      # Buy for $100, sell for $50
/adminshop setprice iron_ingot 10 5     # Buy for $10, sell for $5
/adminshop setprice emerald 200 0       # Buy for $200, cannot sell
```

### Chest Shop

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/chestshop create` | Start shop creation mode | `/chestshop create` | `vonixcore.shops.create` |
| `/chestshop remove` | Remove your shop | `/chestshop remove` | `vonixcore.shops.create` |
| `/chestshop cancel` | Cancel shop creation | `/chestshop cancel` | `vonixcore.shops.create` |

**How to create a chest shop:**
1. Put items in a chest (the first item type will be your shop's item)
2. Run `/chestshop create`
3. Right-click the chest
4. Enter **buy price** in chat (price players pay to buy from you) - type `0` or `skip` to disable buying
5. Enter **sell price** in chat (price you pay players who sell to you) - type `0` or `skip` to disable selling

**Visual Features:**
- **Shop Sign**: Automatically placed on the front of the chest showing item name and prices
- **Item Hologram**: A floating, glowing item display appears above the chest

**Player Interactions:**
- Right-click a shop to view info and buy
- Sneak + right-click to sell items to the shop (if shop has both buy and sell prices)
- Owners can break their shop chests while sneaking

### Player Market

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/market` | Open market GUI | `/market` | `vonixcore.market` |
| `/market browse` | Browse listings | `/market browse` | `vonixcore.market` |
| `/market create <price>` | List held item | `/market create 500` | `vonixcore.market` |
| `/market my` | View your listings | `/market my` | `vonixcore.market` |
| `/market search <query>` | Search listings | `/market search diamond` | `vonixcore.market` |
| `/market collect` | Collect earnings | `/market collect` | `vonixcore.market` |

---

## Jobs Commands

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/jobs` | Open jobs GUI | `/jobs` | `vonixcore.jobs` |
| `/jobs list` | List all jobs | `/jobs list` | `vonixcore.jobs` |
| `/jobs join <job>` | Join a job | `/jobs join miner` | `vonixcore.jobs` |
| `/jobs leave <job>` | Leave a job | `/jobs leave miner` | `vonixcore.jobs` |
| `/jobs stats` | View your job stats | `/jobs stats` | `vonixcore.jobs` |
| `/jobs info <job>` | View job details | `/jobs info miner` | `vonixcore.jobs` |

**Available Jobs:** Miner, Woodcutter, Farmer, Hunter, Fisherman, Builder

---

## Protection Commands

All protection commands use CoreProtect-style syntax.

### Inspector Mode

| Command | Description | Usage |
|---------|-------------|-------|
| `/co inspect` | Toggle inspector mode | `/co inspect` |
| `/co i` | Shorthand for inspect | `/co i` |

In inspector mode, click blocks to see their history.

### Lookup

| Command | Description | Usage |
|---------|-------------|-------|
| `/co lookup` | Lookup nearby changes | `/co lookup` |
| `/co l <params>` | Lookup with parameters | `/co l u:Steve t:1h r:10` |

**Parameters:**
- `u:<user>` - Filter by username
- `t:<time>` - Time range (e.g., `1h`, `7d`, `30m`)
- `r:<radius>` - Search radius in blocks
- `b:<block>` - Filter by block type
- `a:<action>` - Filter by action (`break`, `place`)

**Examples:**
```
/co l u:Steve t:24h
/co l r:20 b:diamond_ore
/co l u:Steve t:1h a:break
```

### Rollback

| Command | Description | Usage |
|---------|-------------|-------|
| `/co rollback` | Rollback changes | `/co rollback u:Steve t:1h r:20` |
| `/co rb` | Shorthand for rollback | `/co rb u:Steve t:1h` |
| `/co undo` | Undo last rollback | `/co undo` |

**Examples:**
```
/co rb u:Steve t:1h r:50        # Rollback Steve's changes, last hour, 50 block radius
/co rb t:30m r:10 a:break       # Rollback all breaks in last 30 min, 10 block radius
```

### Restore

| Command | Description | Usage |
|---------|-------------|-------|
| `/co restore` | Restore rolled-back changes | `/co restore u:Steve t:1h` |
| `/co rs` | Shorthand for restore | `/co rs u:Steve t:1h` |

### Other

| Command | Description | Usage |
|---------|-------------|-------|
| `/co status` | View protection status | `/co status` |
| `/co near [radius]` | View changes near you | `/co near 5` |
| `/co purge <time>` | Purge old data | `/co purge 30d` |

---

## Graves Commands

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/graves` | List your graves | `/graves` | - |
| `/graves list` | List your graves | `/graves list` | - |
| `/graves status` | View system status | `/graves status` | `vonixcore.admin` |

Graves are created automatically when you die: items are stored in a chest at your death location.

---

## Authentication Commands

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/login <password>` | Login to your account | `/login MyPassword123` | - |
| `/register` | Get registration code | `/register` | - |
| `/register <password>` | Register with password | `/register MyPassword123` | - |

---

## Utility Commands

### Player Information

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/ping` | Check your ping | `/ping` | - |
| `/near [radius]` | View nearby players | `/near 100` | - |
| `/whois <player>` | View player info | `/whois Steve` | - |
| `/seen <player>` | Check if player online | `/seen Steve` | - |
| `/getpos` | Get your coordinates | `/getpos` | - |
| `/playtime` | View your playtime | `/playtime` | - |

### Nickname

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/nick <name>` | Set nickname | `/nick &6GoldName` | `vonixcore.nick` |
| `/nick` | Clear nickname | `/nick` | `vonixcore.nick` |

Supports color codes with `&`.

### Messaging

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/msg <player> <message>` | Private message | `/msg Steve Hello!` | - |
| `/r <message>` | Reply to message | `/r Got it!` | - |
| `/ignore <player>` | Toggle ignore player | `/ignore Steve` | - |

---

## Admin Commands

### Player Management

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/heal [player]` | Heal player | `/heal Steve` | `vonixcore.heal` |
| `/feed [player]` | Feed player | `/feed Steve` | `vonixcore.feed` |
| `/fly [player]` | Toggle fly mode | `/fly Steve` | `vonixcore.fly` |
| `/god [player]` | Toggle god mode | `/god Steve` | `vonixcore.god` |
| `/speed <0-10>` | Set walk/fly speed | `/speed 5` | `vonixcore.speed` |

### Inventory

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/clear [player]` | Clear inventory | `/clear Steve` | `vonixcore.clear` |
| `/invsee <player>` | View player inventory | `/invsee Steve` | `vonixcore.invsee` |
| `/enderchest [player]` | Open ender chest | `/enderchest Steve` | `vonixcore.enderchest` |

### Items

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/repair` | Repair held item | `/repair` | `vonixcore.repair` |
| `/more` | Fill item stack | `/more` | `vonixcore.more` |
| `/hat` | Wear item as hat | `/hat` | `vonixcore.hat` |

### Server

| Command | Description | Usage | Permission |
|---------|-------------|-------|------------|
| `/broadcast <message>` | Broadcast message | `/broadcast Hello!` | `vonixcore.broadcast` |
| `/gc` | View server stats | `/gc` | `vonixcore.gc` |
| `/lag` | View server TPS | `/lag` | - |
| `/workbench` | Open crafting table | `/workbench` | - |

---

## Command Aliases

Many commands have shorter aliases:

| Command | Aliases |
|---------|---------|
| `/balance` | `/bal`, `/money` |
| `/msg` | `/tell`, `/whisper`, `/w` |
| `/reply` | `/r` |
| `/coreprotect` | `/co`, `/vp` |
| `/broadcast` | `/bc` |
| `/clear` | `/clearinventory`, `/ci` |
| `/enderchest` | `/ec` |
| `/workbench` | `/craft` |
| `/getpos` | `/pos`, `/coords` |
| `/graves` | `/grave` |
| `/market` | `/auction`, `/ah` |
| `/jobs` | `/job` |
