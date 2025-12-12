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

## 🛒 Shop System

VonixCore includes a comprehensive shop system with multiple shop types.

### GUI Shops

Open the server shop or player market with a visual inventory interface:

```
/shop          - Open server/admin shop
/shop server   - Open server shop (buy from server)
/shop player   - Open player market (buy from players)
/market        - Alias for /shop player
```

**How to use:**
- Left-click items to buy
- Right-click items to sell (server shop only)
- Shift+click to buy/sell in bulk (64 at a time)

### Chest Shops

Create physical chest-based shops:

```
/chestshop create  - Start shop creation, then click a chest
/chestshop remove  - Remove your shop (or break it while sneaking)
/chestshop info    - View shop details
/chestshop cancel  - Cancel shop creation
```

**How chest shops work:**
- Click a chest shop to see options
- Regular click = buy
- Sneak + click = sell to shop

### Sign Shops (Admin-only)

Create sign-based shops by placing a sign with this format:
```
Line 1: [Buy] or [Sell]
Line 2: <quantity>
Line 3: <item name>
Line 4: $<price>
```

Example:
```
[Buy]
16
diamond
$500
```

### Quick Sell

Sell items directly to the server:

```
/sell hand  - Sell the item you're holding
/sell all   - Sell all sellable items in your inventory
```

### Admin Shop Commands

Set server shop prices (requires `vonixcore.adminshop`):

```
/adminshop setprice <item> <buy_price> <sell_price>
/adminshop list
```

**Example:**
```
/adminshop setprice diamond 100 50
> Diamond: Buy $100, Sell $50
```

### Economy Plan Import/Export

Bulk import or export admin shop prices using JSON files:

```
/eco import [filename]   - Import prices from JSON file
/eco export [filename]   - Export current prices to JSON file
```

**Default file:** `config/vonixcore/economy_plan.json`

**JSON Format:**
```json
{
  "version": 1,
  "items": [
    {"id": "minecraft:diamond", "buy": 40.00, "sell": 8.00},
    {"id": "minecraft:netherite_ingot", "buy": 200.00, "sell": null},
    {"id": "minecraft:rotten_flesh", "buy": null, "sell": 0.05}
  ]
}
```

| Field | Description |
|-------|-------------|
| `id` | Full item ID (namespace:item) |
| `buy` | Buy price, or `null` = cannot buy |
| `sell` | Sell price, or `null` = cannot sell |

### Daily Rewards

```
/daily  - Claim your daily login reward
```

- Rewards increase with consecutive daily logins (streak)
- Maximum streak bonus at 7 days
- Missing a day resets your streak

---

## 🔑 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `vonixcore.balance` | Check own balance | All players |
| `vonixcore.balance.others` | Check others' balance | OP |
| `vonixcore.pay` | Pay other players | All players |
| `vonixcore.baltop` | View leaderboard | All players |
| `vonixcore.eco` | Admin economy commands | OP |
| `vonixcore.shop` | Use GUI shops | All players |
| `vonixcore.shop.sell` | List items on player market | All players |
| `vonixcore.sell` | Quick sell commands | All players |
| `vonixcore.chestshop` | Create chest shops | All players |
| `vonixcore.adminshop` | Manage server shop prices | OP |
| `vonixcore.daily` | Claim daily rewards | All players |

---

## 📊 Database Storage

Economy data is stored in multiple tables:

**Player Balances** (`vonixcore_economy`):
| Column | Type | Description |
|--------|------|-------------|
| `uuid` | VARCHAR(36) | Player UUID (primary key) |
| `username` | VARCHAR(16) | Last known username |
| `balance` | DOUBLE | Current balance |
| `last_transaction` | BIGINT | Timestamp of last activity |

**Chest Shops** (`vonixcore_chest_shops`):
| Column | Type | Description |
|--------|------|-------------|
| `id` | INT | Shop ID |
| `owner` | VARCHAR(36) | Owner UUID |
| `world` | VARCHAR | World name |
| `x, y, z` | INT | Chest position |
| `item_id` | VARCHAR | Item being sold |
| `buy_price` | DOUBLE | Price to buy |
| `sell_price` | DOUBLE | Price shop pays |
| `stock` | INT | Items in stock |

**Player Market Listings** (`vonixcore_player_listings`):
| Column | Type | Description |
|--------|------|-------------|
| `id` | INT | Listing ID |
| `seller` | VARCHAR(36) | Seller UUID |
| `item_id` | VARCHAR | Item ID |
| `quantity` | INT | Number for sale |
| `price` | DOUBLE | Total price |

---

## 🔌 API Integration

For plugin developers, the economy can be accessed programmatically:

```java
EconomyManager eco = EconomyManager.getInstance();

// Get balance
double balance = eco.getBalance(playerUUID);

// Modify balance
eco.deposit(playerUUID, 100.0);
eco.withdraw(playerUUID, 50.0);
eco.setBalance(playerUUID, 1000.0);

// Transfer between players
eco.transfer(fromUUID, toUUID, amount);
```

---

## 💡 Tips

1. **Starting Balance**: Set appropriately for your server's economy scale
2. **Max Balance**: Use to prevent economy inflation
3. **Currency Symbol**: Keep short for cleaner display
4. **Regular Backups**: Backup database for economy safety
5. **Admin Shop Prices**: Set sell prices lower than buy to create a money sink

---

## 🔗 Related Documentation

- [Commands Reference](commands.md)
- [Configuration Guide](configuration.md)
