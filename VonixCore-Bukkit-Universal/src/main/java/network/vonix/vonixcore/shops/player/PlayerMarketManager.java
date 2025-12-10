package network.vonix.vonixcore.shops.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.TransactionLog;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Player Market - Players can list items for sale and browse other listings.
 * Based on auction house / player shop concepts.
 */
public class PlayerMarketManager implements Listener {

    private final VonixCore plugin;

    // All active listings
    private final Map<Long, PlayerListing> listings = new ConcurrentHashMap<>();

    // Player earnings waiting to be collected
    private final Map<UUID, Double> pendingEarnings = new ConcurrentHashMap<>();

    // Track open market GUIs
    private final Map<UUID, MarketSession> openSessions = new HashMap<>();

    private BukkitTask expirationTask;

    public PlayerMarketManager(VonixCore plugin) {
        this.plugin = plugin;
    }

    public void initialize(Connection conn) throws SQLException {
        createTables(conn);
        loadListings();
        loadPendingEarnings();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startExpirationTask();
    }

    private void createTables(Connection conn) throws SQLException {
        // Listings table
        conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS vonixcore_market_listings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT,
                    item_type TEXT NOT NULL,
                    item_data TEXT,
                    quantity INTEGER NOT NULL,
                    price_each REAL NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    sold INTEGER DEFAULT 0,
                    collected INTEGER DEFAULT 0
                )
                """);

        // Pending earnings table
        conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS vonixcore_market_earnings (
                    uuid TEXT PRIMARY KEY,
                    amount REAL NOT NULL DEFAULT 0
                )
                """);

        // Indexes
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_market_seller ON vonixcore_market_listings(seller_uuid)");
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_market_item ON vonixcore_market_listings(item_type)");
    }

    private void loadListings() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                // Only load non-expired listings
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT * FROM vonixcore_market_listings WHERE expires_at > ? AND sold < quantity");
                stmt.setLong(1, System.currentTimeMillis());
                ResultSet rs = stmt.executeQuery();

                int loaded = 0;
                while (rs.next()) {
                    PlayerListing listing = listingFromResultSet(rs);
                    listings.put(listing.getId(), listing);
                    loaded++;
                }
                plugin.getLogger().info("[Shops] Loaded " + loaded + " market listings");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load market listings", e);
            }
        });
    }

    private void loadPendingEarnings() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT * FROM vonixcore_market_earnings WHERE amount > 0");
                while (rs.next()) {
                    pendingEarnings.put(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getDouble("amount"));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load pending earnings", e);
            }
        });
    }

    private void startExpirationTask() {
        // Check for expired listings every minute
        expirationTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            List<Long> expired = new ArrayList<>();

            for (PlayerListing listing : listings.values()) {
                if (listing.isExpired()) {
                    expired.add(listing.getId());
                }
            }

            for (Long id : expired) {
                PlayerListing listing = listings.remove(id);
                if (listing != null) {
                    // Return unsold items to seller (add to pending collection)
                    // For now, just remove from active listings
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        Player seller = Bukkit.getPlayer(listing.getSellerUuid());
                        if (seller != null && seller.isOnline()) {
                            seller.sendMessage(Component.text("Your listing for " + listing.getItemType() +
                                    " has expired!").color(NamedTextColor.YELLOW));
                        }
                    });
                }
            }
        }, 20 * 60, 20 * 60);
    }

    /**
     * Create a new listing
     */
    public boolean createListing(Player player, ItemStack item, double priceEach) {
        // Check max listings
        int current = getPlayerListingCount(player.getUniqueId());
        if (current >= ShopsConfig.playerMarketMaxListings && !player.hasPermission("vonixcore.market.unlimited")) {
            player.sendMessage(Component.text("You have reached the maximum number of listings!")
                    .color(NamedTextColor.RED));
            return false;
        }

        // Validate price
        if (priceEach < ShopsConfig.playerMarketMinPrice) {
            player.sendMessage(Component.text("Price must be at least " +
                    EconomyManager.getInstance().format(ShopsConfig.playerMarketMinPrice))
                    .color(NamedTextColor.RED));
            return false;
        }
        if (priceEach > ShopsConfig.playerMarketMaxPrice) {
            player.sendMessage(Component.text("Price cannot exceed " +
                    EconomyManager.getInstance().format(ShopsConfig.playerMarketMaxPrice))
                    .color(NamedTextColor.RED));
            return false;
        }

        // Calculate expiration
        long expiresAt = System.currentTimeMillis() + (ShopsConfig.playerMarketListingDurationHours * 60 * 60 * 1000L);

        // Create listing
        PlayerListing listing = PlayerListing.create(
                player.getUniqueId(), player.getName(), item, priceEach, expiresAt);

        // Remove item from player
        player.getInventory().removeItem(item);

        // Save to database
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                long id = insertListing(conn, listing);
                listing.setId(id);
                listings.put(id, listing);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Listing created! " + item.getAmount() + "x " +
                            item.getType().name() + " for " + EconomyManager.getInstance().format(priceEach) + " each")
                            .color(NamedTextColor.GREEN));
                });
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create listing", e);
                // Give item back
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.getInventory().addItem(item);
                    player.sendMessage(Component.text("Failed to create listing!").color(NamedTextColor.RED));
                });
            }
        });

        return true;
    }

    /**
     * Purchase from a listing
     */
    public boolean purchase(Player buyer, PlayerListing listing, int amount) {
        if (listing.isSoldOut() || listing.isExpired()) {
            buyer.sendMessage(Component.text("This listing is no longer available!").color(NamedTextColor.RED));
            return false;
        }

        int available = listing.getRemaining();
        int toBuy = Math.min(amount, available);
        double totalCost = listing.getPriceEach() * toBuy;
        double tax = totalCost * ShopsConfig.playerMarketTaxRate;
        double sellerReceives = totalCost - tax;

        EconomyManager eco = EconomyManager.getInstance();

        // Check buyer has funds
        if (!eco.has(buyer.getUniqueId(), totalCost)) {
            buyer.sendMessage(Component.text("Not enough money! Need: " + eco.format(totalCost))
                    .color(NamedTextColor.RED));
            return false;
        }

        // Process payment
        eco.withdraw(buyer.getUniqueId(), totalCost);

        // Add to seller's pending earnings
        double currentEarnings = pendingEarnings.getOrDefault(listing.getSellerUuid(), 0.0);
        pendingEarnings.put(listing.getSellerUuid(), currentEarnings + sellerReceives);

        // Update listing
        listing.setSold(listing.getSold() + toBuy);

        // Give items to buyer
        ItemStack purchaseItem = listing.getItem();
        purchaseItem.setAmount(toBuy);
        HashMap<Integer, ItemStack> leftover = buyer.getInventory().addItem(purchaseItem);
        for (ItemStack left : leftover.values()) {
            buyer.getWorld().dropItemNaturally(buyer.getLocation(), left);
        }

        // Log transaction
        TransactionLog.getInstance().logMarketPurchase(
                buyer.getUniqueId(), listing.getSellerUuid(), totalCost, tax,
                listing.getItemType(), toBuy);

        // Save changes
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                updateListingSold(conn, listing);
                updatePendingEarnings(conn, listing.getSellerUuid(),
                        pendingEarnings.get(listing.getSellerUuid()));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to update listing", e);
            }
        });

        // Notify seller if online
        if (ShopsConfig.playerMarketNotifyOnSale) {
            Player seller = Bukkit.getPlayer(listing.getSellerUuid());
            if (seller != null && seller.isOnline()) {
                seller.sendMessage(Component.text(buyer.getName() + " purchased " + toBuy + "x " +
                        listing.getItemType() + " from your listing!").color(NamedTextColor.GREEN));
            }
        }

        buyer.sendMessage(Component.text("Purchased " + toBuy + "x " + listing.getItemType() +
                " for " + eco.format(totalCost)).color(NamedTextColor.GREEN));

        return true;
    }

    /**
     * Collect pending earnings
     */
    public double collectEarnings(Player player) {
        double amount = pendingEarnings.getOrDefault(player.getUniqueId(), 0.0);
        if (amount <= 0)
            return 0;

        EconomyManager.getInstance().deposit(player.getUniqueId(), amount);
        pendingEarnings.put(player.getUniqueId(), 0.0);

        // Save to database
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                updatePendingEarnings(conn, player.getUniqueId(), 0);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to update earnings", e);
            }
        });

        return amount;
    }

    /**
     * Open the market browse GUI
     */
    public void openBrowseMenu(Player player, int page, String filter) {
        List<PlayerListing> activeListings = listings.values().stream()
                .filter(l -> !l.isExpired() && !l.isSoldOut())
                .filter(l -> filter == null || l.getItemType().toLowerCase().contains(filter.toLowerCase()))
                .sorted(Comparator.comparingLong(PlayerListing::getCreatedAt).reversed())
                .collect(Collectors.toList());

        int totalPages = (activeListings.size() / 45) + 1;
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Market - Page " + (page + 1) + "/" + totalPages));

        // Add listings
        int start = page * 45;
        int end = Math.min(start + 45, activeListings.size());

        for (int i = start; i < end; i++) {
            PlayerListing listing = activeListings.get(i);
            ItemStack display = createListingDisplay(listing);
            inv.setItem(i - start, display);
        }

        // Navigation
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.displayName(Component.text("Previous Page").color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            prev.setItemMeta(meta);
            inv.setItem(45, prev);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.displayName(Component.text("Next Page").color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            next.setItemMeta(meta);
            inv.setItem(53, next);
        }

        // My Listings
        ItemStack myListings = new ItemStack(Material.CHEST);
        ItemMeta meta = myListings.getItemMeta();
        meta.displayName(Component.text("My Listings").color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        myListings.setItemMeta(meta);
        inv.setItem(49, myListings);

        // Collect earnings
        double earnings = pendingEarnings.getOrDefault(player.getUniqueId(), 0.0);
        ItemStack collect = new ItemStack(earnings > 0 ? Material.GOLD_INGOT : Material.IRON_INGOT);
        meta = collect.getItemMeta();
        meta.displayName(Component.text("Collect Earnings").color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Pending: " + EconomyManager.getInstance().format(earnings))
                        .color(earnings > 0 ? NamedTextColor.GOLD : NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        collect.setItemMeta(meta);
        inv.setItem(50, collect);

        // Track session
        MarketSession session = new MarketSession();
        session.currentView = "browse";
        session.currentPage = page;
        session.filter = filter;
        session.displayedListings = activeListings.subList(start, end);
        openSessions.put(player.getUniqueId(), session);

        player.openInventory(inv);
    }

    /**
     * Open player's own listings
     */
    public void openMyListings(Player player) {
        List<PlayerListing> myListings = listings.values().stream()
                .filter(l -> l.getSellerUuid().equals(player.getUniqueId()))
                .sorted(Comparator.comparingLong(PlayerListing::getCreatedAt).reversed())
                .collect(Collectors.toList());

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("My Listings"));

        for (int i = 0; i < Math.min(45, myListings.size()); i++) {
            PlayerListing listing = myListings.get(i);
            ItemStack display = createMyListingDisplay(listing);
            inv.setItem(i, display);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text("Back to Market").color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inv.setItem(49, back);

        MarketSession session = new MarketSession();
        session.currentView = "mylistings";
        session.displayedListings = myListings.subList(0, Math.min(45, myListings.size()));
        openSessions.put(player.getUniqueId(), session);

        player.openInventory(inv);
    }

    private ItemStack createListingDisplay(PlayerListing listing) {
        Material mat = Material.getMaterial(listing.getItemType());
        if (mat == null)
            mat = Material.BARRIER;

        ItemStack item = new ItemStack(mat, Math.min(listing.getRemaining(), 64));
        ItemMeta meta = item.getItemMeta();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Seller: ").color(NamedTextColor.GRAY)
                .append(Component.text(listing.getSellerName()).color(NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Price: ").color(NamedTextColor.GRAY)
                .append(Component.text(EconomyManager.getInstance().format(listing.getPriceEach()) + " each")
                        .color(NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Available: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(listing.getRemaining())).color(NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Left-click: Buy 1").color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift-click: Buy all").color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMyListingDisplay(PlayerListing listing) {
        Material mat = Material.getMaterial(listing.getItemType());
        if (mat == null)
            mat = Material.BARRIER;

        ItemStack item = new ItemStack(mat, Math.min(listing.getQuantity(), 64));
        ItemMeta meta = item.getItemMeta();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Price: ").color(NamedTextColor.GRAY)
                .append(Component.text(EconomyManager.getInstance().format(listing.getPriceEach()) + " each")
                        .color(NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Sold: ").color(NamedTextColor.GRAY)
                .append(Component.text(listing.getSold() + "/" + listing.getQuantity()).color(NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Earnings: ").color(NamedTextColor.GRAY)
                .append(Component.text(EconomyManager.getInstance().format(listing.getEarnings()))
                        .color(NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));

        if (listing.isExpired()) {
            lore.add(Component.text("EXPIRED").color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Shift-click to cancel").color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        MarketSession session = openSessions.get(player.getUniqueId());
        if (session == null)
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize())
            return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        if (session.currentView.equals("browse")) {
            handleBrowseClick(player, slot, event.getClick(), session);
        } else if (session.currentView.equals("mylistings")) {
            handleMyListingsClick(player, slot, event.getClick(), session);
        }
    }

    private void handleBrowseClick(Player player, int slot, ClickType click, MarketSession session) {
        // Navigation
        if (slot == 45 && session.currentPage > 0) {
            openBrowseMenu(player, session.currentPage - 1, session.filter);
            return;
        }
        if (slot == 53) {
            openBrowseMenu(player, session.currentPage + 1, session.filter);
            return;
        }
        if (slot == 49) {
            openMyListings(player);
            return;
        }
        if (slot == 50) {
            double earned = collectEarnings(player);
            if (earned > 0) {
                player.sendMessage(Component.text("Collected " + EconomyManager.getInstance().format(earned))
                        .color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("No earnings to collect!").color(NamedTextColor.YELLOW));
            }
            openBrowseMenu(player, session.currentPage, session.filter);
            return;
        }

        // Listing purchase
        if (slot < session.displayedListings.size()) {
            PlayerListing listing = session.displayedListings.get(slot);

            // Can't buy own listings
            if (listing.getSellerUuid().equals(player.getUniqueId())) {
                player.sendMessage(Component.text("You can't buy your own listing!").color(NamedTextColor.RED));
                return;
            }

            int amount = click.isShiftClick() ? listing.getRemaining() : 1;
            purchase(player, listing, amount);
            openBrowseMenu(player, session.currentPage, session.filter);
        }
    }

    private void handleMyListingsClick(Player player, int slot, ClickType click, MarketSession session) {
        if (slot == 49) {
            openBrowseMenu(player, 0, null);
            return;
        }

        if (click.isShiftClick() && slot < session.displayedListings.size()) {
            PlayerListing listing = session.displayedListings.get(slot);
            cancelListing(player, listing);
            openMyListings(player);
        }
    }

    private void cancelListing(Player player, PlayerListing listing) {
        // Return unsold items
        int remaining = listing.getRemaining();
        if (remaining > 0) {
            ItemStack returnItem = listing.getItem();
            returnItem.setAmount(remaining);
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(returnItem);
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }

        listings.remove(listing.getId());

        // Delete from database
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM vonixcore_market_listings WHERE id = ?");
                stmt.setLong(1, listing.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete listing", e);
            }
        });

        player.sendMessage(Component.text("Listing cancelled. " + remaining + " items returned.")
                .color(NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openSessions.remove(event.getPlayer().getUniqueId());
    }

    // Query methods
    public int getPlayerListingCount(UUID player) {
        return (int) listings.values().stream()
                .filter(l -> l.getSellerUuid().equals(player))
                .count();
    }

    public List<PlayerListing> searchListings(String query) {
        return listings.values().stream()
                .filter(l -> !l.isExpired() && !l.isSoldOut())
                .filter(l -> l.getItemType().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Database methods
    private long insertListing(Connection conn, PlayerListing listing) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                """
                        INSERT INTO vonixcore_market_listings
                        (seller_uuid, seller_name, item_type, item_data, quantity, price_each, created_at, expires_at, sold, collected)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, listing.getSellerUuid().toString());
        stmt.setString(2, listing.getSellerName());
        stmt.setString(3, listing.getItemType());
        stmt.setString(4, listing.getItemData());
        stmt.setInt(5, listing.getQuantity());
        stmt.setDouble(6, listing.getPriceEach());
        stmt.setLong(7, listing.getCreatedAt());
        stmt.setLong(8, listing.getExpiresAt());
        stmt.setInt(9, listing.getSold());
        stmt.setInt(10, listing.isCollected() ? 1 : 0);
        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next())
            return rs.getLong(1);
        throw new SQLException("Failed to get generated ID");
    }

    private void updateListingSold(Connection conn, PlayerListing listing) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE vonixcore_market_listings SET sold = ? WHERE id = ?");
        stmt.setInt(1, listing.getSold());
        stmt.setLong(2, listing.getId());
        stmt.executeUpdate();
    }

    private void updatePendingEarnings(Connection conn, UUID player, double amount) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR REPLACE INTO vonixcore_market_earnings (uuid, amount) VALUES (?, ?)");
        stmt.setString(1, player.toString());
        stmt.setDouble(2, amount);
        stmt.executeUpdate();
    }

    private PlayerListing listingFromResultSet(ResultSet rs) throws SQLException {
        PlayerListing listing = new PlayerListing();
        listing.setId(rs.getLong("id"));
        listing.setSellerUuid(UUID.fromString(rs.getString("seller_uuid")));
        listing.setSellerName(rs.getString("seller_name"));
        listing.setItemType(rs.getString("item_type"));
        listing.setItemData(rs.getString("item_data"));
        listing.setQuantity(rs.getInt("quantity"));
        listing.setPriceEach(rs.getDouble("price_each"));
        listing.setCreatedAt(rs.getLong("created_at"));
        listing.setExpiresAt(rs.getLong("expires_at"));
        listing.setSold(rs.getInt("sold"));
        listing.setCollected(rs.getInt("collected") == 1);
        return listing;
    }

    public void shutdown() {
        if (expirationTask != null) {
            expirationTask.cancel();
        }
        openSessions.clear();
    }

    public void reload() {
        // Reload from database
    }

    private static class MarketSession {
        String currentView = "browse";
        int currentPage = 0;
        String filter;
        List<PlayerListing> displayedListings = new ArrayList<>();
    }
}
