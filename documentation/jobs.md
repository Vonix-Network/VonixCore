# Jobs System

VonixCore includes a comprehensive jobs system that allows players to earn money and points by performing in-game activities.

## Overview

- **8 Built-in Jobs** - Miner, Woodcutter, Farmer, Hunter, Builder, Fisherman, Digger, Crafter
- **11 Action Types** - Break, Place, Kill, Fish, Craft, Smelt, Brew, Enchant, Breed, Tame, Shear
- **Points System** - Earn job-specific points alongside money
- **Leveling** - Gain XP and level up for better rewards
- **Anti-Exploit** - Placing ores gives negative rewards (prevents break→place farming)

## Available Jobs

| Job | Icon | Activities | Starting Reward |
|-----|------|------------|-----------------|
| **Miner** | Diamond Pickaxe | Mine ores, stone, deepslate | $0.50 - $20 |
| **Woodcutter** | Diamond Axe | Chop all log types | $1.00 - $2.50 |
| **Farmer** | Diamond Hoe | Harvest crops, breed animals | $0.75 - $5.00 |
| **Hunter** | Diamond Sword | Kill mobs (Warden = $50!) | $2.00 - $50 |
| **Builder** | Bricks | Place blocks | $0.25 - $2.00 |
| **Fisherman** | Fishing Rod | Catch fish & treasures | $2.00 - $25 |
| **Digger** | Iron Shovel | Dig soil, sand, gravel | $0.25 - $2.00 |
| **Crafter** | Crafting Table | Craft items, smelt | $0.10 - $15 |

## Commands

| Command | Description |
|---------|-------------|
| `/jobs` | Show jobs help |
| `/jobs list` | List all available jobs |
| `/jobs join <job>` | Join a job (max 3) |
| `/jobs leave <job>` | Leave a job |
| `/jobs stats` | View your job progress |
| `/jobs info <job>` | View job details |

## Action Types

| Action | Description | Jobs Using |
|--------|-------------|------------|
| `BREAK` | Breaking blocks | Miner, Woodcutter, Farmer, Digger |
| `PLACE` | Placing blocks | Builder, Miner (anti-exploit) |
| `KILL` | Killing entities | Hunter |
| `FISH` | Catching fish/treasures | Fisherman |
| `CRAFT` | Crafting items | Crafter |
| `SMELT` | Smelting in furnace | Crafter |
| `BREED` | Breeding animals | Farmer |
| `TAME` | Taming animals | Farmer |

## Three-Currency System

Jobs reward three currencies:
- **Income** → Goes to economy balance ($)
- **Experience** → Levels up the job
- **Points** → Job-specific currency for future perks

## Anti-Exploit Protection

To prevent place→break farming, the Miner job has **negative rewards** for placing ores:

| Action | Block | Reward |
|--------|-------|--------|
| Break | Diamond Ore | +$10 |
| **Place** | **Diamond Ore** | **-$10** |

This means if you place a diamond ore and break it, you net $0.

## Leveling System

- Higher levels unlock income/XP multipliers
- Income: +2% per level
- Experience: +1% per level
- Level formula: `100 * level + level² * 10`

## Configuration

Location: `config/vonixcore/jobs.json`

```json
{
  "enabled": true,
  "maxJobs": 3,
  "globalIncomeMultiplier": 1.0,
  "globalExpMultiplier": 1.0
}
```

### Config in `vonixcore-essentials.toml`
```toml
[features]
jobs_enabled = true
```

## Detailed Job Rewards

### Miner
- Stone: $0.50 | Deepslate: $0.75
- Coal Ore: $2.00 | Deepslate Coal: $2.50
- Iron Ore: $3.00 | Deepslate Iron: $4.00
- Diamond Ore: $10.00 | Deepslate Diamond: $12.00
- Emerald Ore: $12.00 | Ancient Debris: $20.00

### Fisherman
- Cod/Salmon: $2.00 - $2.50
- Tropical Fish: $5.00
- Enchanted Book: $20.00
- Nautilus Shell: $25.00

### Hunter
- Zombies/Spiders: $2.00
- Creepers: $3.00
- Endermen: $5.00
- Wither Skeletons: $8.00
- Ravager: $15.00
- **Warden: $50.00**

### Crafter
- Any craft: $0.10
- Iron tools: $2-3
- Diamond tools: $5-12
- Diamond block: $10
- Smelting: $0.50-5.00 per ingot

## Tips

1. **Choose Complementary Jobs** - Miner + Crafter works well
2. **Focus on High-Value Actions** - Diamond mining > stone
3. **Avoid Placing Ores** - You'll lose money!
4. **Fish for Treasures** - Enchanted books pay well
5. **Hunt the Warden** - High risk, high reward ($50!)
