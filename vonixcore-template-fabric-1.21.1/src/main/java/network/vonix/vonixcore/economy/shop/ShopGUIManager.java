package network.vonix.vonixcore.economy.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages GUI shop sessions and interactions.
 * Fabric 1.20.1 compatible version.
 */
public class ShopGUIManager {

    private static ShopGUIManager instance;

    // Track active shop sessions
    private final Map<UUID, ShopSession> activeSessions = new ConcurrentHashMap<>();

    public static ShopGUIManager getInstance() {
        if (instance == null) {
            instance = new ShopGUIManager();
        }
        return instance;
    }

    /**
     * Open the admin shop GUI for a player
     */
    public void openAdminShop(ServerPlayer player) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§6§lServer Shop");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
                ShopMenu menu = new ShopMenu(containerId, playerInventory, ShopMenu.ShopType.ADMIN_SHOP);
                menu.populateAdminShop(0);
                menu.setClickHandler(event -> handleAdminShopClick(event));

                // Track session
                activeSessions.put(p.getUUID(), new ShopSession(ShopMenu.ShopType.ADMIN_SHOP, menu));

                return menu;
            }
        });
    }

    /**
     * Open the player market GUI
     */
    public void openPlayerMarket(ServerPlayer player) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§a§lPlayer Market");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
                ShopMenu menu = new ShopMenu(containerId, playerInventory, ShopMenu.ShopType.PLAYER_MARKET);
                menu.populatePlayerMarket(0);
                menu.setClickHandler(event -> handlePlayerMarketClick(event));

                activeSessions.put(p.getUUID(), new ShopSession(ShopMenu.ShopType.PLAYER_MARKET, menu));

                return menu;
            }
        });
    }

    /**
     * Handle admin shop click events
     */
    private void handleAdminShopClick(ShopMenu.ShopClickEvent event) {
        ServerPlayer player = event.player();
        int slot = event.slotId();

        // Navigation buttons
        if (slot == 45 && event.currentPage() > 0) {
            // Previous page
            ShopSession session = activeSessions.get(player.getUUID());
            if (session != null && session.menu instanceof ShopMenu shopMenu) {
                shopMenu.populateAdminShop(event.currentPage() - 1);
            }
            return;
        }

        if (slot == 53) {
            // Next page
            ShopSession session = activeSessions.get(player.getUUID());
            if (session != null && session.menu instanceof ShopMenu shopMenu) {
                shopMenu.populateAdminShop(event.currentPage() + 1);
            }
            return;
        }

        if (slot == 49) {
            // Info item - do nothing
            return;
        }

        // Item interaction
        if (slot >= 0 && slot < 45 && !event.clickedItem().isEmpty()) {
            String itemId = ItemUtils.getItemId(event.clickedItem());
            ShopManager.AdminShopItem shopItem = ShopManager.getInstance().getAdminPrice(itemId);

            if (shopItem == null) {
                player.sendSystemMessage(Component.literal("§cThis item is not available in the shop."));
                return;
            }

            String symbol = EssentialsConfig.getInstance().getCurrencySymbol();

            if (event.isLeftClick()) {
                // Buy
                if (shopItem.buyPrice() == null || shopItem.buyPrice() <= 0) {
                    player.sendSystemMessage(Component.literal("§cThis item cannot be purchased."));
                    return;
                }

                double price = shopItem.buyPrice();
                double balance = EconomyManager.getInstance().getBalance(player.getUUID());

                if (balance < price) {
                    player.sendSystemMessage(
                            Component.literal("§cInsufficient funds! Need " + symbol + String.format("%.2f", price)));
                    return;
                }

                // Take money and give item
                if (EconomyManager.getInstance().withdraw(player.getUUID(), price)) {
                    var leftover = ItemUtils.giveItems(player, itemId, 1);
                    if (!leftover.isEmpty()) {
                        // Drop items that didn't fit
                        player.drop(leftover, false);
                    }
                    player.sendSystemMessage(Component
                            .literal("§aPurchased 1x " + itemId + " for " + symbol + String.format("%.2f", price)));
                }

            } else if (event.isRightClick()) {
                // Sell
                if (shopItem.sellPrice() == null || shopItem.sellPrice() <= 0) {
                    player.sendSystemMessage(Component.literal("§cThis item cannot be sold."));
                    return;
                }

                int playerHas = ItemUtils.countItems(player, itemId);
                if (playerHas < 1) {
                    player.sendSystemMessage(Component.literal("§cYou don't have any of this item to sell."));
                    return;
                }

                double price = shopItem.sellPrice();

                // Take item and give money
                if (ItemUtils.removeItems(player, itemId, 1)) {
                    EconomyManager.getInstance().deposit(player.getUUID(), price);
                    player.sendSystemMessage(
                            Component.literal("§aSold 1x " + itemId + " for " + symbol + String.format("%.2f", price)));
                }

            } else if (event.isShiftClick() && event.isLeftClick()) {
                // Bulk buy (stack)
                if (shopItem.buyPrice() == null || shopItem.buyPrice() <= 0) {
                    player.sendSystemMessage(Component.literal("§cThis item cannot be purchased."));
                    return;
                }

                int amount = 64;
                double totalPrice = shopItem.buyPrice() * amount;
                double balance = EconomyManager.getInstance().getBalance(player.getUUID());

                int affordable = (int) (balance / shopItem.buyPrice());
                int toBuy = Math.min(amount, affordable);

                if (toBuy <= 0) {
                    player.sendSystemMessage(Component.literal("§cInsufficient funds!"));
                    return;
                }

                totalPrice = shopItem.buyPrice() * toBuy;

                if (EconomyManager.getInstance().withdraw(player.getUUID(), totalPrice)) {
                    var leftover = ItemUtils.giveItems(player, itemId, toBuy);
                    if (!leftover.isEmpty()) {
                        player.drop(leftover, false);
                    }
                    player.sendSystemMessage(Component.literal("§aPurchased " + toBuy + "x " + itemId + " for " + symbol
                            + String.format("%.2f", totalPrice)));
                }
            }
        }
    }

    /**
     * Handle player market click events
     */
    private void handlePlayerMarketClick(ShopMenu.ShopClickEvent event) {
        ServerPlayer player = event.player();
        int slot = event.slotId();

        // Navigation
        if (slot == 45 && event.currentPage() > 0) {
            ShopSession session = activeSessions.get(player.getUUID());
            if (session != null && session.menu instanceof ShopMenu shopMenu) {
                shopMenu.populatePlayerMarket(event.currentPage() - 1);
            }
            return;
        }

        if (slot == 53) {
            ShopSession session = activeSessions.get(player.getUUID());
            if (session != null && session.menu instanceof ShopMenu shopMenu) {
                shopMenu.populatePlayerMarket(event.currentPage() + 1);
            }
            return;
        }

        if (slot == 49) {
            return;
        }

        // Purchase listing
        if (slot >= 0 && slot < 45 && !event.clickedItem().isEmpty()) {
            List<ShopManager.PlayerListing> listings = ShopManager.getInstance().getAllListings();
            int index = (event.currentPage() * 45) + slot;

            if (index >= listings.size()) {
                return;
            }

            ShopManager.PlayerListing listing = listings.get(index);
            String symbol = EssentialsConfig.getInstance().getCurrencySymbol();

            // Can't buy your own listing
            if (listing.seller().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cYou can't buy your own listing!"));
                return;
            }

            double balance = EconomyManager.getInstance().getBalance(player.getUUID());
            if (balance < listing.price()) {
                player.sendSystemMessage(Component
                        .literal("§cInsufficient funds! Need " + symbol + String.format("%.2f", listing.price())));
                return;
            }

            // Process purchase
            if (ShopManager.getInstance().buyListing(listing.id(), player.getUUID())) {
                var leftover = ItemUtils.giveItems(player, listing.itemId(), listing.quantity());
                if (!leftover.isEmpty()) {
                    player.drop(leftover, false);
                }
                player.sendSystemMessage(Component.literal("§aPurchased " + listing.quantity() + "x " + listing.itemId()
                        + " for " + symbol + String.format("%.2f", listing.price())));

                // Refresh the GUI
                ShopSession session = activeSessions.get(player.getUUID());
                if (session != null && session.menu instanceof ShopMenu shopMenu) {
                    shopMenu.populatePlayerMarket(event.currentPage());
                }
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to purchase listing. It may have been sold."));
            }
        }
    }

    /**
     * Clean up session when player closes menu
     */
    public void onPlayerCloseMenu(UUID uuid) {
        activeSessions.remove(uuid);
    }

    /**
     * Check if player has active shop session
     */
    public boolean hasActiveSession(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    /**
     * Shop session data
     */
    private record ShopSession(ShopMenu.ShopType type, AbstractContainerMenu menu) {
    }
}
