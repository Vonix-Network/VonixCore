# VonixCore Economy System

VonixCore includes a full-featured economy system with persistent balances, transactions, and admin controls.

---

## 📋 Overview

The economy system provides:
- Persistent player balances (SQLite/MySQL)
- Player-to-player payments
- Balance leaderboard
- Admin commands for managing economy
- Configurable currency names and symbols

---

## ⚙️ Configuration

**File:** `vonixcore-essentials.toml`

```toml
[economy]
# Enable economy system
enabled = true

# Starting balance for new players
starting_balance = 100.0

# Currency symbol (displayed before amounts)
currency_symbol = "$"

# Currency name (singular)
currency_name = "Dollar"

# Currency name (plural)
currency_name_plural = "Dollars"

# Minimum transaction amount
min_transaction = 0.01

# Maximum balance a player can have (0 = unlimited)
max_balance = 0
```

### Example Configurations

**Default (Dollars)**
```toml
currency_symbol = "$"
currency_name = "Dollar"
currency_name_plural = "Dollars"
```

**Coins**
```toml
currency_symbol = "⛃"
currency_name = "Coin"
currency_name_plural = "Coins"
```

**Credits**
```toml
currency_symbol = "¢"
currency_name = "Credit"
currency_name_plural = "Credits"
```

---

## 💰 Player Commands

### Check Balance

```
/balance
/bal
```

Shows your current balance.

```
/balance <player>
/bal <player>
```

Shows another player's balance (requires `vonixcore.balance.others`).

### Pay Players

```
/pay <player> <amount>
```

Send money to another player.

**Example:**
```
/pay Steve 100
> You paid Steve $100.00
```

### Balance Leaderboard

```
/baltop
/baltop <page>
```

View the richest players on the server.

---

## 🔧 Admin Commands

All admin commands require the `vonixcore.eco` permission.

### Give Money

```
/eco give <player> <amount>
```

Add money to a player's balance.

### Take Money

```
/eco take <player> <amount>
```

Remove money from a player's balance.

### Set Balance

```
/eco set <player> <amount>
```

Set a player's balance to a specific amount.

### Reset Economy

```
/eco reset <player>
```

Reset a player's balance to the starting amount.

---

## 🔑 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `vonixcore.balance` | Check own balance | All players |
| `vonixcore.balance.others` | Check others' balance | OP |
| `vonixcore.pay` | Pay other players | All players |
| `vonixcore.baltop` | View leaderboard | All players |
| `vonixcore.eco` | Admin economy commands | OP |
| `vonixcore.eco.give` | Give money | OP |
| `vonixcore.eco.take` | Take money | OP |
| `vonixcore.eco.set` | Set balance | OP |

---

## 📊 Database Storage

Economy data is stored in the `vonixcore_economy` table:

| Column | Type | Description |
|--------|------|-------------|
| `uuid` | VARCHAR(36) | Player UUID (primary key) |
| `username` | VARCHAR(16) | Last known username |
| `balance` | DOUBLE | Current balance |
| `last_transaction` | BIGINT | Timestamp of last activity |

---

## 🔌 API Integration

For plugin developers, the economy can be accessed programmatically:

```java
EconomyManager eco = EconomyManager.getInstance();

// Get balance
double balance = eco.getBalance(playerUUID);

// Modify balance
eco.addBalance(playerUUID, 100.0);
eco.removeBalance(playerUUID, 50.0);
eco.setBalance(playerUUID, 1000.0);

// Check if player can afford
boolean canAfford = eco.hasBalance(playerUUID, 200.0);
```

---

## 💡 Tips

1. **Starting Balance**: Set appropriately for your server's economy scale
2. **Max Balance**: Use to prevent economy inflation
3. **Currency Symbol**: Keep short for cleaner display
4. **Regular Backups**: Backup database for economy safety

---

## 🔗 Related Documentation

- [Commands Reference](commands.md)
- [Configuration Guide](configuration.md)
