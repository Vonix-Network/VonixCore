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
 * Forge 1.20.1 compatible version.
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

        if (slot == 45 && event.currentPage() > 0) {
            ShopSession session = activeSessions.get(player.getUUID());
            if (session != null && session.menu instanceof ShopMenu shopMenu) {
                shopMenu.populateAdminShop(event.currentPage() - 1);
            }
            return;
        }

        if (slot == 53) {
            ShopSession session = activeSessions.get(player.getUUID());
            if (session != null && session.menu instanceof ShopMenu shopMenu) {
                shopMenu.populateAdminShop(event.currentPage() + 1);
            }
            return;
        }

        if (slot == 49) {
            return;
        }

        if (slot >= 0 && slot < 45 && !event.clickedItem().isEmpty()) {
            String itemId = ItemUtils.getItemId(event.clickedItem());
            ShopManager.AdminShopItem shopItem = ShopManager.getInstance().getAdminPrice(itemId);

            if (shopItem == null) {
                player.sendSystemMessage(Component.literal("§cThis item is not available in the shop."));
                return;
            }

            String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

            if (event.isLeftClick() && !event.isShiftClick()) { // Buy 1
                if (shopItem.buyPrice() == null || shopItem.buyPrice() <= 0) {
                    player.sendSystemMessage(Component.literal("§cThis item cannot be purchased."));
                    return;
                }
                double price = shopItem.buyPrice();
                EconomyManager.getInstance().has(player.getUUID(), price).thenAccept(hasFunds -> {
                    if (hasFunds) {
                        EconomyManager.getInstance().withdraw(player.getUUID(), price).thenAccept(withdrew -> {
                            if (withdrew) {
                                VonixCore.execute(() -> {
                                    var leftover = ItemUtils.giveItems(player, itemId, 1);
                                    if (!leftover.isEmpty()) player.drop(leftover, false);
                                    player.sendSystemMessage(Component.literal("§aPurchased 1x " + itemId + " for " + symbol + String.format("%.2f", price)));
                                });
                            }
                        });
                    } else {
                        player.sendSystemMessage(Component.literal("§cInsufficient funds! Need " + symbol + String.format("%.2f", price)));
                    }
                });

            } else if (event.isRightClick()) { // Sell 1
                if (shopItem.sellPrice() == null || shopItem.sellPrice() <= 0) {
                    player.sendSystemMessage(Component.literal("§cThis item cannot be sold."));
                    return;
                }
                if (ItemUtils.countItems(player, itemId) < 1) {
                    player.sendSystemMessage(Component.literal("§cYou don't have any of this item to sell."));
                    return;
                }
                double price = shopItem.sellPrice();
                if (ItemUtils.removeItems(player, itemId, 1)) {
                    EconomyManager.getInstance().deposit(player.getUUID(), price);
                    player.sendSystemMessage(Component.literal("§aSold 1x " + itemId + " for " + symbol + String.format("%.2f", price)));
                }

            } else if (event.isShiftClick() && event.isLeftClick()) { // Bulk Buy
                if (shopItem.buyPrice() == null || shopItem.buyPrice() <= 0) {
                    player.sendSystemMessage(Component.literal("§cThis item cannot be purchased."));
                    return;
                }
                int amount = 64;
                double pricePerItem = shopItem.buyPrice();
                EconomyManager.getInstance().getBalance(player.getUUID()).thenAccept(balance -> {
                    int affordable = (int) (balance / pricePerItem);
                    int toBuy = Math.min(amount, affordable);

                    if (toBuy <= 0) {
                        player.sendSystemMessage(Component.literal("§cInsufficient funds!"));
                        return;
                    }

                    double totalPrice = pricePerItem * toBuy;
                    EconomyManager.getInstance().withdraw(player.getUUID(), totalPrice).thenAccept(withdrew -> {
                        if (withdrew) {
                            VonixCore.execute(() -> {
                                var leftover = ItemUtils.giveItems(player, itemId, toBuy);
                                if (!leftover.isEmpty()) player.drop(leftover, false);
                                player.sendSystemMessage(Component.literal("§aPurchased " + toBuy + "x " + itemId + " for " + symbol + String.format("%.2f", totalPrice)));
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

        if (slot >= 0 && slot < 45 && !event.clickedItem().isEmpty()) {
            ShopManager.getInstance().getAllListings().thenAccept(listings -> {
                int index = (event.currentPage() * 45) + slot;
                if (index >= listings.size()) return;

                ShopManager.PlayerListing listing = listings.get(index);
                String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

                if (listing.seller().equals(player.getUUID())) {
                    player.sendSystemMessage(Component.literal("§cYou can't buy your own listing!"));
                    return;
                }

                EconomyManager.getInstance().has(player.getUUID(), listing.price()).thenAccept(hasFunds -> {
                    if (hasFunds) {
                        ShopManager.getInstance().buyListing(listing.id(), player.getUUID()).thenAccept(purchased -> {
                            if (purchased) {
                                VonixCore.execute(() -> {
                                    var leftover = ItemUtils.giveItems(player, listing.itemId(), listing.quantity());
                                    if (!leftover.isEmpty()) player.drop(leftover, false);

                                    player.sendSystemMessage(Component.literal("§aPurchased " + listing.quantity() + "x " + listing.itemId() + " for " + symbol + String.format("%.2f", listing.price())));

                                    if (network.vonix.vonixcore.config.ShopsConfig.CONFIG.transactionLogEnabled.get()) {
                                        double taxRate = network.vonix.vonixcore.config.ShopsConfig.CONFIG.playerMarketTaxRate.get();
                                        double tax = listing.price() * taxRate;
                                        network.vonix.vonixcore.economy.TransactionLog.getInstance().logMarketPurchase(player.getUUID(), listing.seller(), listing.price(), tax, listing.itemId(), listing.quantity());
                                    }

                                    if (network.vonix.vonixcore.config.ShopsConfig.CONFIG.playerMarketNotifyOnSale.get()) {
                                        var server = player.getServer();
                                        if (server != null) {
                                            ServerPlayer seller = server.getPlayerList().getPlayer(listing.seller());
                                            if (seller != null) {
                                                double taxRate = network.vonix.vonixcore.config.ShopsConfig.CONFIG.playerMarketTaxRate.get();
                                                double tax = listing.price() * taxRate;
                                                seller.sendSystemMessage(Component.literal("§a[Market] §eYour " + listing.quantity() + "x " + listing.itemId() + " sold for " + symbol + String.format("%.2f", listing.price() - tax) + "!"));
                                            }
                                        }
                                    }

                                    ShopSession session = activeSessions.get(player.getUUID());
                                    if (session != null && session.menu instanceof ShopMenu shopMenu) {
                                        shopMenu.populatePlayerMarket(event.currentPage());
                                    }
                                });
                            } else {
                                player.sendSystemMessage(Component.literal("§cFailed to purchase listing. It may have been sold or an error occurred."));
                            }
                        });
                    } else {
                        player.sendSystemMessage(Component.literal("§cInsufficient funds! Need " + symbol + String.format("%.2f", listing.price())));
                    }
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
