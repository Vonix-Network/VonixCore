# Claims System

VonixCore includes a land claims protection system that allows players to protect their builds while still allowing shop functionality.

## Overview

The claims system provides:
- **Land Protection** - Prevent block break/place by non-trusted players
- **Container Protection** - Prevent chest/barrel access by non-trusted players
- **Explosion Protection** - Prevent TNT/creeper damage to claims
- **Trust System** - Allow specific players to build in your claims
- **Shop Integration** - VonixCore shops work inside claims for all players

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/vcclaims wand` | Get claim selection wand (golden shovel) | `vonixcore.claims.create` |
| `/vcclaims create [radius]` | Create claim from selection or radius | `vonixcore.claims.create` |
| `/vcclaims delete` | Delete claim you're standing in | Owner or admin |
| `/vcclaims trust <player>` | Trust player in current claim | Owner |
| `/vcclaims untrust <player>` | Remove player trust | Owner |
| `/vcclaims list` | List your claims | - |
| `/vcclaims info` | Show info about current claim | - |
| `/vcclaims admin delete <id>` | Admin delete any claim | `vonixcore.claims.admin` |

Command aliases: `/vonixcoreclaims`, `/claims`

## Creating Claims

### Method 1: Wand Selection
1. Run `/vcclaims wand` to receive a golden shovel
2. Left-click a block to set corner 1
3. Right-click a block to set corner 2
4. Run `/vcclaims create`

### Method 2: Radius
1. Stand at the center of your desired claim
2. Run `/vcclaims create <radius>` (e.g., `/vcclaims create 10`)

The claim extends from Y=-64 to Y=320 (full world height).

## Trust System

Trust allows other players to build and access containers in your claim:

```
/vcclaims trust PlayerName    - Add trust
/vcclaims untrust PlayerName  - Remove trust
```

Trusted players can:
- Break and place blocks
- Open chests and other containers
- Interact with entities

## Shop Integration

When `allowVonixShopsBypass` is enabled (default):
- **Chest Shops** - Non-trusted players can buy/sell from chest shops in claims
- **Sign Shops** - Non-trusted players can use sign shops in claims
- The chest inventory itself remains protected - only shop transactions work

This allows shop owners to set up protected shops that customers can use.

## Configuration

### NeoForge/Forge
File: `vonixcore-claims.toml`

```toml
[claims]
enabled = true

[claims.limits]
defaultClaimRadius = 10
maxClaimSize = 100
maxClaimsPerPlayer = 5

[claims.permissions]
requirePermissionToCreate = false

[claims.protection]
protectBuilding = true
protectContainers = true
protectEntities = true
preventExplosions = true
preventFireSpread = true

[claims.integration]
allowVonixShopsBypass = true
```

### Bukkit
File: `config.yml`

```yaml
claims:
  enabled: true
  defaultClaimRadius: 10
  maxClaimSize: 100
  maxClaimsPerPlayer: 5
  requirePermissionToCreate: false
  protectBuilding: true
  protectContainers: true
  preventExplosions: true
  allowVonixShopsBypass: true
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `vonixcore.claims.create` | Create land claims | true |
| `vonixcore.claims.admin` | Admin bypass and commands | op |

## Database

Claims are stored in the `vonixcore_claims` table with:
- Owner UUID and name
- World name
- Corner coordinates (x1,y1,z1) to (x2,y2,z2)
- Trusted players (JSON array)
- Creation timestamp

## Non-Interference

The VonixCore claims system is designed to **not interfere** with other protection plugins:
- It only protects areas with active VonixCore claims
- Areas outside claims are unaffected
- Other protection plugins (GriefPrevention, WorldGuard, etc.) can coexist
