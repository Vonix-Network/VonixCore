# Jobs System

VonixCore includes a jobs system that allows players to earn money by performing in-game activities.

## Overview

The jobs system provides:
- **Multiple Jobs** - Miner, Woodcutter, Farmer, Hunter, Builder
- **Leveling** - Gain experience and level up for better rewards
- **Income Multipliers** - Higher levels earn more per action
- **Configurable** - Server owners can adjust rewards and settings

## Available Jobs

| Job | Icon | Activities | Starting Reward |
|-----|------|------------|-----------------|
| **Miner** | Diamond Pickaxe | Mine ores and stone | $0.50 - $12 per block |
| **Woodcutter** | Diamond Axe | Chop logs | $1.00 - $2.00 per log |
| **Farmer** | Diamond Hoe | Harvest crops | $1.00 - $3.00 per crop |
| **Hunter** | Diamond Sword | Kill mobs | $2.00 - $12 per kill |
| **Builder** | Bricks | Place blocks | $0.25 - $1.50 per block |

## Commands

| Command | Description |
|---------|-------------|
| `/jobs` | Show jobs help |
| `/jobs list` | List all available jobs |
| `/jobs join <job>` | Join a job |
| `/jobs leave <job>` | Leave a job |
| `/jobs stats` | View your job stats |
| `/jobs info <job>` | View job details |

## Joining Jobs

Players can have up to 3 jobs simultaneously (configurable).

```
/jobs list              - See available jobs
/jobs join miner        - Join the miner job
/jobs stats             - View your progress
```

## Earning Rewards

Once you join a job, you automatically earn money and experience for:

### Miner
- Stone: $0.50 + 1.0 XP
- Coal Ore: $2.00 + 3.0 XP
- Iron Ore: $3.00 + 5.0 XP
- Gold Ore: $5.00 + 8.0 XP
- Diamond Ore: $10.00 + 15.0 XP
- Emerald Ore: $12.00 + 20.0 XP

### Woodcutter
- All Logs: $1.00 - $2.00 + 2.0-3.0 XP

### Farmer
- Wheat, Carrots, Potatoes: $1.00 + 1.5 XP
- Beetroots: $1.50 + 2.0 XP
- Nether Wart: $2.00 + 3.0 XP

### Hunter
- Zombies, Spiders: $2.00 - $2.50 + 3.0-4.0 XP
- Creepers: $3.00 + 5.0 XP
- Endermen: $5.00 + 8.0 XP
- Blazes: $6.00 + 10.0 XP
- Wither Skeletons: $8.00 + 12.0 XP

### Builder
- Any Block: $0.25 + 0.5 XP
- Bricks: $1.00 + 1.5 XP
- Stone Bricks: $0.75 + 1.0 XP

## Leveling System

As you earn experience, you level up:
- Higher levels unlock income multipliers
- Each level requires progressively more XP
- Level-up notifications appear in chat

## Configuration

### NeoForge/Forge
File: `config/vonixcore/jobs.json`

```json
{
  "enabled": true,
  "maxJobs": 3,
  "globalIncomeMultiplier": 1.0,
  "globalExpMultiplier": 1.0
}
```

Also in `vonixcore-essentials.toml`:
```toml
[features]
jobs_enabled = true
```

### Bukkit
The Jobs system uses the same configuration approach. Check `vonixcore-essentials.yml`.

## Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `enabled` | Enable jobs system | true |
| `maxJobs` | Maximum jobs per player | 3 |
| `globalIncomeMultiplier` | Multiply all job income | 1.0 |
| `globalExpMultiplier` | Multiply all job XP | 1.0 |

## Anti-Exploit

- Only natural actions count (placing then breaking doesn't exploit Builder+Miner)
- Rewards are per block/kill, not per item drop
- Server can adjust multipliers to balance economy

## Tips

1. **Choose Complementary Jobs** - Miner pairs well with Builder
2. **Focus on High-Value Actions** - Diamond mining pays better than stone
3. **Level Up** - Higher levels mean better income
4. **Max 3 Jobs** - Choose wisely based on your playstyle
