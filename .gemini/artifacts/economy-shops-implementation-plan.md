# VonixCore Economy & Shops System - Implementation Plan

## Overview

Enhance VonixCore's economy system to become a comprehensive, all-in-one solution rivaling best-in-class plugins like QuickShop-Hikari, EconomyCore, and VaultUnlocked.

## Features to Implement

### 1. Core Economy Enhancements
- [x] Basic balance management (already exists)
- [ ] Multi-currency support (like EconomyCore)
- [x] Transaction logging/history ✅ IMPLEMENTED (Bukkit, NeoForge, Forge)
- [ ] Vault API compatibility provider
- [ ] Economy placeholder (PAPI) support

### 2. Chest Shops (QuickShop-style)
- [x] Click chest + hold item to create shop ✅ IMPLEMENTED (Bukkit)
- [x] Shop signs on chest showing item/price ✅ IMPLEMENTED
- [x] Buy/Sell shop types ✅ IMPLEMENTED
- [x] Admin shops (unlimited stock) ✅ IMPLEMENTED
- [x] Item display on top of chest ✅ IMPLEMENTED
- [x] Shop protection (only owner can break) ✅ IMPLEMENTED
- [x] Shop permissions (staff/friends can manage) ✅ IMPLEMENTED
- [x] Shop search command ✅ IMPLEMENTED

### 3. Sign Shops
- [x] Create shop by placing sign with [Buy] or [Sell] ✅ IMPLEMENTED (Bukkit)
- [x] Format: Line 1: [Buy/Sell], Line 2: Quantity, Line 3: Price, Line 4: Item ✅ IMPLEMENTED
- [x] Works without chest (uses virtual storage for admin shops) ✅ IMPLEMENTED
- [x] Sign protection ✅ IMPLEMENTED

### 4. Server GUI Shop (Admin Shop System)
- [x] `/shop` command opens GUI ✅ IMPLEMENTED (Bukkit)
- [x] Categories (Tools, Armor, Food, Blocks, etc.) ✅ IMPLEMENTED
- [x] Configurable items via YAML config ✅ IMPLEMENTED
- [x] Buy and Sell prices configurable separately ✅ IMPLEMENTED
- [x] Unlimited stock (admin shop) ✅ IMPLEMENTED
- [x] Beautiful GUI with item lore showing prices ✅ IMPLEMENTED

### 5. Player GUI Shop (Player Market/Auction)
- [x] `/pshop` - Player's personal shop GUI ✅ IMPLEMENTED (Bukkit)
- [x] Players can list items for sale ✅ IMPLEMENTED
- [x] Browse other players' shops ✅ IMPLEMENTED
- [x] Search functionality ✅ IMPLEMENTED
- [x] Expiring listings (configurable) ✅ IMPLEMENTED
- [x] Collect earnings from sales ✅ IMPLEMENTED
- [x] Max listings per player (configurable) ✅ IMPLEMENTED

---

## Database Schema

### vonixcore_economy (existing)
```sql
uuid TEXT PRIMARY KEY,
balance REAL DEFAULT 0,
username TEXT
```

### vonixcore_chest_shops (new)
```sql
id INTEGER PRIMARY KEY AUTOINCREMENT,
owner_uuid TEXT NOT NULL,
world TEXT NOT NULL,
x INTEGER NOT NULL,
y INTEGER NOT NULL,
z INTEGER NOT NULL,
item_type TEXT NOT NULL,
item_data TEXT, -- NBT data for special items
price REAL NOT NULL,
shop_type TEXT DEFAULT 'SELL', -- SELL, BUY, SELL_BUY
is_admin BOOLEAN DEFAULT FALSE,
stock INTEGER DEFAULT 0,
created_at INTEGER,
last_transaction INTEGER
```

### vonixcore_sign_shops (new)
```sql
id INTEGER PRIMARY KEY AUTOINCREMENT,
owner_uuid TEXT NOT NULL,
world TEXT NOT NULL,
x INTEGER NOT NULL,
y INTEGER NOT NULL,
z INTEGER NOT NULL,
item_type TEXT NOT NULL,
quantity INTEGER DEFAULT 1,
price REAL NOT NULL,
shop_type TEXT DEFAULT 'BUY',
is_admin BOOLEAN DEFAULT FALSE,
created_at INTEGER
```

### vonixcore_gui_shop_items (new - for server admin shop config)
```sql
id INTEGER PRIMARY KEY AUTOINCREMENT,
category TEXT NOT NULL,
item_type TEXT NOT NULL,
item_data TEXT,
buy_price REAL DEFAULT -1, -- -1 means cannot buy
sell_price REAL DEFAULT -1, -- -1 means cannot sell
slot INTEGER DEFAULT -1,
display_name TEXT,
lore TEXT
```

### vonixcore_player_listings (new - for player market)
```sql
id INTEGER PRIMARY KEY AUTOINCREMENT,
seller_uuid TEXT NOT NULL,
item_type TEXT NOT NULL,
item_data TEXT,
quantity INTEGER NOT NULL,
price_each REAL NOT NULL,
created_at INTEGER,
expires_at INTEGER,
sold INTEGER DEFAULT 0
```

### vonixcore_transactions (new)
```sql
id INTEGER PRIMARY KEY AUTOINCREMENT,
from_uuid TEXT,
to_uuid TEXT,
amount REAL NOT NULL,
type TEXT, -- DEPOSIT, WITHDRAW, TRANSFER, SHOP_BUY, SHOP_SELL, ADMIN_SET
description TEXT,
timestamp INTEGER
```

---

## Commands

### Economy (existing + new)
- `/balance [player]` - View balance
- `/pay <player> <amount>` - Transfer money
- `/baltop [page]` - Rich list
- `/eco set/give/take <player> <amount>` - Admin commands
- `/transactions [player]` - View transaction history (NEW)

### Chest Shops
- `/cshop create <price> [buy|sell]` - Create shop (while looking at chest)
- `/cshop remove` - Remove shop (while looking at shop)
- `/cshop setprice <price>` - Update price
- `/cshop find <item>` - Find shops selling item
- `/cshop staff add/remove <player>` - Manage staff
- `/cshop info` - View shop info

### Sign Shops
- Created by placing signs with format:
  ```
  [Buy] or [Sell]
  <quantity>
  <price>
  <item>
  ```
- `/signshop remove` - Remove while looking at sign

### Server GUI Shop
- `/shop` - Open server shop GUI
- `/shop buy <item> [amount]` - Quick buy
- `/shop sell <item> [amount]` - Quick sell (or hand)
- `/shop sellall` - Sell all sellable items in inventory
- `/shopadmin reload` - Reload shop config
- `/shopadmin setprice <item> <buy> <sell>` - Set prices

### Player Shop (Market)
- `/pshop` - Open your shop listings
- `/pshop create <price> [amount]` - List item in hand
- `/pshop browse` - Browse all player shops
- `/pshop search <item>` - Search listings
- `/pshop collect` - Collect earnings
- `/market` - Alias for `/pshop browse`

---

## File Structure

```
vonixcore-bukkit-universal/src/main/java/network/vonix/vonixcore/
├── economy/
│   ├── EconomyManager.java (enhanced)
│   ├── EconomyCommands.java (enhanced)
│   ├── TransactionLog.java (NEW)
│   ├── VaultProvider.java (NEW - Vault API hook)
│   └── EconomyPlaceholders.java (NEW)
├── shops/
│   ├── ShopsManager.java (NEW - orchestrator)
│   ├── chest/
│   │   ├── ChestShop.java (NEW - shop data class)
│   │   ├── ChestShopManager.java (NEW)
│   │   ├── ChestShopListener.java (NEW)
│   │   ├── ChestShopCommands.java (NEW)
│   │   └── ChestShopDisplay.java (NEW - item display)
│   ├── sign/
│   │   ├── SignShop.java (NEW)
│   │   ├── SignShopManager.java (NEW)
│   │   └── SignShopListener.java (NEW)
│   ├── gui/
│   │   ├── ServerShop.java (NEW - admin GUI shop)
│   │   ├── ServerShopGUI.java (NEW)
│   │   ├── ServerShopConfig.java (NEW)
│   │   └── ServerShopCommands.java (NEW)
│   └── player/
│       ├── PlayerShop.java (NEW - player market)
│       ├── PlayerShopManager.java (NEW)
│       ├── PlayerShopGUI.java (NEW)
│       ├── PlayerListing.java (NEW)
│       └── PlayerShopCommands.java (NEW)
```

---

## Configuration

### vonixcore-shops.toml
```toml
[shops]
enabled = true

[shops.chest]
enabled = true
max_per_player = 50
tax_rate = 0.05  # 5% tax on sales
admin_only_create = false
allow_buy_type = true
allow_sell_type = true
display_items = true  # Show floating item above chest
protect_chests = true

[shops.sign]
enabled = true
max_per_player = 25
require_permission = false

[shops.gui]
enabled = true
menu_title = "&6Server Shop"
# Categories and items defined in shops-gui.yml

[shops.player]
enabled = true
max_listings = 10
listing_duration_hours = 168  # 1 week
tax_rate = 0.10  # 10% tax
min_price = 1.0
max_price = 1000000.0
```

### shops-gui.yml (Server Admin Shop Items)
```yaml
categories:
  tools:
    name: "&6Tools"
    icon: DIAMOND_PICKAXE
    items:
      - type: DIAMOND_PICKAXE
        buy: 500
        sell: 100
      - type: DIAMOND_AXE
        buy: 400
        sell: 80
  blocks:
    name: "&aBlocks"
    icon: GRASS_BLOCK
    items:
      - type: COBBLESTONE
        buy: 1
        sell: 0.5
      - type: STONE
        buy: 2
        sell: 1
  food:
    name: "&cFood"
    icon: COOKED_BEEF
    items:
      - type: BREAD
        buy: 5
        sell: 2
```

---

## Implementation Priority

### Phase 1: Core Infrastructure
1. Transaction logging system
2. Enhanced EconomyManager with events
3. Vault API provider (for compatibility)
4. Database schema updates

### Phase 2: Chest Shops
1. ChestShop data model
2. ChestShopManager (CRUD operations)
3. ChestShopListener (creation, interaction, protection)
4. ChestShopCommands
5. Item display system

### Phase 3: Sign Shops
1. SignShop data model
2. SignShopManager
3. SignShopListener

### Phase 4: Server GUI Shop
1. Config loading
2. GUI rendering
3. Buy/Sell functionality
4. Commands

### Phase 5: Player Market
1. PlayerListing data model
2. PlayerShopManager
3. Browse/Search GUI
4. Expiration scheduler

---

## Key Design Decisions

### Inspiration from QuickShop-Hikari:
- Clean Shop interface with clear buy/sell separation
- Shop events for extensibility
- Shop permissions system
- Benefit/tax system

### Inspiration from EconomyCore:
- Multi-currency potential
- Transaction history
- Clean formatting

### Inspiration from VaultUnlocked:
- Vault legacy API compatibility
- Modern Vault2 API support
- Service registration pattern

### VonixCore Philosophy:
- All-in-one: No external dependencies required
- Toggleable: Each feature can be disabled
- Optimized: Efficient database queries, caching
- User-friendly: Clear messages, intuitive GUIs
