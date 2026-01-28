package network.vonix.vonixcore.economy.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;
import network.vonix.vonixcore.config.ShopsConfig;
import network.vonix.vonixcore.economy.TransactionLog;
import network.vonix.vonixcore.economy.shop.ItemUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages GUI shop sessions and interactions.
 * Forge 1.18.2 compatible version.
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
                return new TextComponent("§6§lServer Shop");
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
                return new TextComponent("§a§lPlayer Market");
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
                player.sendMessage(new TextComponent("§cThis item is not available in the shop."), player.getUUID());
                return;
            }

            String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

            if (event.isLeftClick()) {
                // Buy
                if (shopItem.buyPrice() == null || shopItem.buyPrice() <= 0) {
                    player.getServer().execute(() -> player.sendMessage(new TextComponent("§cThis item cannot be purchased."), player.getUUID()));
                    return;
                }

                double price = shopItem.buyPrice();
                EconomyManager.getInstance().getBalance(player.getUUID()).thenAccept(balance -> {
                    if (balance < price) {
                        player.getServer().execute(() -> player.sendMessage(
                                new TextComponent("§cInsufficient funds! Need " + symbol + String.format("%.2f", price)),
                                player.getUUID()));
                        return;
                    }

                    // Take money and give item
                    EconomyManager.getInstance().withdraw(player.getUUID(), price).thenAccept(success -> {
                        if (success) {
                            player.getServer().execute(() -> {
                                var leftover = ItemUtils.giveItems(player, itemId, 1);
                                if (!leftover.isEmpty()) {
                                    player.drop(leftover, false);
                                }
                                player.sendMessage(new TextComponent(
                                        "§aPurchased 1x " + itemId + " for " + symbol + String.format("%.2f", price)),
                                        player.getUUID());
                            });
                        }
                    });
                });

            } else if (event.isRightClick()) {
                // Sell
                if (shopItem.sellPrice() == null || shopItem.sellPrice() <= 0) {
                    player.getServer().execute(() -> player.sendMessage(new TextComponent("§cThis item cannot be sold."), player.getUUID()));
                    return;
                }

                if (ItemUtils.countItems(player, itemId) < 1) {
                    player.getServer().execute(() -> player.sendMessage(new TextComponent("§cYou don't have any of this item to sell."),
                            player.getUUID()));
                    return;
                }

                double price = shopItem.sellPrice();

                // Take item and give money
                if (ItemUtils.removeItems(player, itemId, 1)) {
                    EconomyManager.getInstance().deposit(player.getUUID(), price).thenAccept(success -> {
                        if (success) {
                            player.getServer().execute(() -> player.sendMessage(new TextComponent(
                                    "§aSold 1x " + itemId + " for " + symbol + String.format("%.2f", price)), player.getUUID()));
                        }
                    });
                }

            } else if (event.isShiftClick() && event.isLeftClick()) {
                // Bulk buy (stack)
                if (shopItem.buyPrice() == null || shopItem.buyPrice() <= 0) {
                    player.getServer().execute(() -> player.sendMessage(new TextComponent("§cThis item cannot be purchased."), player.getUUID()));
                    return;
                }

                int amount = 64;
                EconomyManager.getInstance().getBalance(player.getUUID()).thenAccept(balance -> {
                    int affordable = (int) (balance / shopItem.buyPrice());
                    int toBuy = Math.min(amount, affordable);

                    if (toBuy <= 0) {
                        player.getServer().execute(() -> player.sendMessage(new TextComponent("§cInsufficient funds!"), player.getUUID()));
                        return;
                    }

                    double totalPrice = shopItem.buyPrice() * toBuy;

                    EconomyManager.getInstance().withdraw(player.getUUID(), totalPrice).thenAccept(success -> {
                        if (success) {
                            player.getServer().execute(() -> {
                                var leftover = ItemUtils.giveItems(player, itemId, toBuy);
                                if (!leftover.isEmpty()) {
                                    player.drop(leftover, false);
                                }
                                player.sendMessage(new TextComponent("§aPurchased " + toBuy + "x " + itemId + " for " + symbol
                                        + String.format("%.2f", totalPrice)), player.getUUID());
                            });
                        }
                    });
                });
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
            ShopManager.getInstance().getAllListings().thenAccept(listings -> {
                int index = (event.currentPage() * 45) + slot;

                if (index >= listings.size()) {
                    return;
                }

                ShopManager.PlayerListing listing = listings.get(index);
                String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

                // Can't buy your own listing
                if (listing.seller().equals(player.getUUID())) {
                    player.getServer().execute(() -> player.sendMessage(new TextComponent("§cYou can't buy your own listing!"), player.getUUID()));
                    return;
                }

                EconomyManager.getInstance().getBalance(player.getUUID()).thenCompose(balance -> {
                    if (balance < listing.price()) {
                        player.getServer().execute(() -> player.sendMessage(new TextComponent(
                                "§cInsufficient funds! Need " + symbol + String.format("%.2f", listing.price())),
                                player.getUUID()));
                        return CompletableFuture.completedFuture(null);
                    }

                    // Process purchase
                    return ShopManager.getInstance().buyListing(listing.id(), player.getUUID()).thenAccept(success -> {
                        if (success) {
                            player.getServer().execute(() -> {
                                var leftover = ItemUtils.giveItems(player, listing.itemId(), listing.quantity());
                                if (!leftover.isEmpty()) {
                                    player.drop(leftover, false);
                                }
                                player.sendMessage(new TextComponent("§aPurchased " + listing.quantity() + "x " + listing.itemId()
                                        + " for " + symbol + String.format("%.2f", listing.price())), player.getUUID());

                                // Log transaction
                                if (ShopsConfig.CONFIG.transactionLogEnabled.get()) {
                                    double taxRate = ShopsConfig.CONFIG.playerMarketTaxRate.get();
                                    double tax = listing.price() * taxRate;
                                    TransactionLog.getInstance().logMarketPurchase(
                                            player.getUUID(), listing.seller(), listing.price(), tax,
                                            listing.itemId(), listing.quantity());
                                }

                                // Notify seller if online and configured
                                if (ShopsConfig.CONFIG.playerMarketNotifyOnSale.get()) {
                                    var server = player.getServer();
                                    if (server != null) {
                                        ServerPlayer seller = server.getPlayerList().getPlayer(listing.seller());
                                        if (seller != null) {
                                            seller.sendMessage(new TextComponent("§a[Market] §eYour " + listing.quantity() + "x "
                                                    + listing.itemId() + " sold for " + symbol + String.format("%.2f", listing.price())
                                                    + "!"), seller.getUUID());
                                        }
                                    }
                                }

                                // Refresh the GUI
                                ShopSession session = activeSessions.get(player.getUUID());
                                if (session != null && session.menu instanceof ShopMenu shopMenu) {
                                    shopMenu.populatePlayerMarket(event.currentPage());
                                }
                            });
                        } else {
                            player.getServer().execute(() -> player.sendMessage(new TextComponent("§cFailed to purchase listing. It may have been sold."),
                                    player.getUUID()));
                        }
                    });
                });
            });
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
