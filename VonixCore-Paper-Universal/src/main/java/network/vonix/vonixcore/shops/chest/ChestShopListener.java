package network.vonix.vonixcore.shops.chest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all chest shop interactions.
 * Based on QuickShop-Hikari's interaction system.
 */
public class ChestShopListener implements Listener {

    private final VonixCore plugin;
    private final ChestShopManager shopManager;

    // Track players in shop creation mode
    private final Map<UUID, ShopCreationData> shopCreationMode = new HashMap<>();

    public ChestShopListener(VonixCore plugin, ChestShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    /**
     * Handle player clicking on chest
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null)
            return;

        // Only handle chests
        if (!isChest(block.getType()))
            return;

        ChestShop shop = shopManager.getShopAt(block);

        // Left click - info or sell to shop
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (shop != null) {
                handleShopInfo(player, shop);
                event.setCancelled(true);
            }
            return;
        }

        // Right click
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack handItem = player.getInventory().getItemInMainHand();

        // Check if in creation mode
        ShopCreationData creationData = shopCreationMode.get(player.getUniqueId());
        if (creationData != null) {
            handleShopCreation(player, block, creationData);
            event.setCancelled(true);
            return;
        }

        // Shop already exists - interact with it
        if (shop != null) {
            handleShopInteraction(player, shop, event.getPlayer().isSneaking());
            event.setCancelled(true);
            return;
        }

        // Player is sneaking with item in hand - initiate shop creation
        if (player.isSneaking() && handItem != null && handItem.getType() != Material.AIR) {
            if (!canCreateShop(player, block)) {
                return;
            }
            initiateShopCreation(player, block, handItem);
            event.setCancelled(true);
        }
    }

    /**
     * Initiate shop creation mode
     */
    private void initiateShopCreation(Player player, Block chestBlock, ItemStack item) {
        // Check admin only create
        if (ShopsConfig.chestShopsAdminOnlyCreate && !player.hasPermission("vonixcore.shops.admin")) {
            player.sendMessage(Component.text("Only admins can create shops!").color(NamedTextColor.RED));
            return;
        }

        // Check max shops
        int current = shopManager.getPlayerShopCount(player.getUniqueId());
        int max = ShopsConfig.chestShopsMaxPerPlayer;
        if (current >= max && !player.hasPermission("vonixcore.shops.unlimited")) {
            player.sendMessage(Component.text("You have reached the maximum number of shops (" + max + ")!")
                    .color(NamedTextColor.RED));
            return;
        }

        ShopCreationData data = new ShopCreationData();
        data.chestLocation = chestBlock.getLocation();
        data.item = item.clone();
        data.step = CreationStep.ENTER_PRICE;
        data.timestamp = System.currentTimeMillis();

        shopCreationMode.put(player.getUniqueId(), data);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Creating Shop ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Item: ").color(NamedTextColor.YELLOW)
                .append(Component.text(item.getType().name()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Enter the price in chat:").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("(Type 'cancel' to cancel)").color(NamedTextColor.GRAY));
    }

    /**
     * Handle shop creation step
     */
    private void handleShopCreation(Player player, Block chestBlock, ShopCreationData data) {
        // Verify same chest
        if (!chestBlock.getLocation().equals(data.chestLocation)) {
            player.sendMessage(Component.text("Please click the same chest or type 'cancel' to cancel.")
                    .color(NamedTextColor.RED));
            return;
        }
    }

    /**
     * Handle chat input for shop creation
     */
    @EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ShopCreationData data = shopCreationMode.get(player.getUniqueId());
        if (data == null)
            return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            shopCreationMode.remove(player.getUniqueId());
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> player.sendMessage(Component.text("Shop creation cancelled.").color(NamedTextColor.YELLOW)));
            return;
        }

        if (data.step == CreationStep.ENTER_PRICE) {
            try {
                double price = Double.parseDouble(message);

                if (price < ShopsConfig.chestShopsMinPrice) {
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> player.sendMessage(Component.text("Price must be at least " +
                                    ShopsConfig.chestShopsMinPrice).color(NamedTextColor.RED)));
                    return;
                }
                if (price > ShopsConfig.chestShopsMaxPrice) {
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> player.sendMessage(Component.text("Price cannot exceed " +
                                    ShopsConfig.chestShopsMaxPrice).color(NamedTextColor.RED)));
                    return;
                }

                data.price = price;
                data.step = CreationStep.SELECT_TYPE;

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Price set to: " + EconomyManager.getInstance().format(price))
                            .color(NamedTextColor.GREEN));
                    player.sendMessage(Component.text("Select shop type:").color(NamedTextColor.YELLOW));
                    player.sendMessage(
                            Component.text("  [1] Selling - You sell items to players").color(NamedTextColor.WHITE));
                    if (ShopsConfig.chestShopsAllowBuyType) {
                        player.sendMessage(Component.text("  [2] Buying - You buy items from players")
                                .color(NamedTextColor.WHITE));
                    }
                    player.sendMessage(Component.text("Enter 1 or 2:").color(NamedTextColor.GREEN));
                });
            } catch (NumberFormatException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> player
                        .sendMessage(Component.text("Invalid price! Enter a number.").color(NamedTextColor.RED)));
            }
            return;
        }

        if (data.step == CreationStep.SELECT_TYPE) {
            ChestShop.ShopType type;
            if (message.equals("1") || message.equalsIgnoreCase("sell") || message.equalsIgnoreCase("selling")) {
                type = ChestShop.ShopType.SELLING;
            } else if ((message.equals("2") || message.equalsIgnoreCase("buy") || message.equalsIgnoreCase("buying"))
                    && ShopsConfig.chestShopsAllowBuyType) {
                type = ChestShop.ShopType.BUYING;
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> player
                        .sendMessage(Component.text("Invalid type! Enter 1 or 2.").color(NamedTextColor.RED)));
                return;
            }

            data.shopType = type;
            shopCreationMode.remove(player.getUniqueId());

            // Create shop on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Block chest = data.chestLocation.getBlock();
                ChestShop shop = shopManager.createShop(player, data.chestLocation, data.item, data.price, type);

                if (shop != null) {
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("✓ Shop created successfully!").color(NamedTextColor.GREEN));
                    player.sendMessage(Component.text("  Item: ").color(NamedTextColor.GRAY)
                            .append(Component.text(data.item.getType().name()).color(NamedTextColor.WHITE)));
                    player.sendMessage(Component.text("  Price: ").color(NamedTextColor.GRAY)
                            .append(Component.text(EconomyManager.getInstance().format(data.price))
                                    .color(NamedTextColor.GOLD)));
                    player.sendMessage(Component.text("  Type: ").color(NamedTextColor.GRAY)
                            .append(Component.text(type.name()).color(NamedTextColor.AQUA)));
                    player.sendMessage(Component.empty());

                    // Create sign on chest if possible
                    createShopSign(chest, shop);
                } else {
                    player.sendMessage(Component.text("Failed to create shop. Please try again.")
                            .color(NamedTextColor.RED));
                }
            });
        }
    }

    /**
     * Handle interacting with an existing shop
     */
    private void handleShopInteraction(Player player, ChestShop shop, boolean sneaking) {
        // Owner/staff access chest normally
        if (shop.hasAccess(player.getUniqueId()) && sneaking) {
            // Allow access to chest
            return;
        }

        // Show shop GUI/interaction
        if (shop.getShopType() == ChestShop.ShopType.SELLING) {
            showBuyInterface(player, shop);
        } else if (shop.getShopType() == ChestShop.ShopType.BUYING) {
            showSellInterface(player, shop);
        }
    }

    /**
     * Show buy interface
     */
    private void showBuyInterface(Player player, ChestShop shop) {
        String formattedPrice = EconomyManager.getInstance().format(shop.getPrice());
        int stock = shop.isUnlimited() ? -1 : shop.getStock();

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Shop Info ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Owner: ").color(NamedTextColor.GRAY)
                .append(Component.text(shop.getOwnerName()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Selling: ").color(NamedTextColor.GRAY)
                .append(Component.text(shop.getItemType()).color(NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Price: ").color(NamedTextColor.GRAY)
                .append(Component.text(formattedPrice + " each").color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Stock: ").color(NamedTextColor.GRAY)
                .append(Component.text(stock == -1 ? "Unlimited" : String.valueOf(stock)).color(NamedTextColor.GREEN)));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Left-click to buy 1, Shift+Left-click for stack")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Type /cshop buy <amount> to buy specific amount")
                .color(NamedTextColor.GRAY));
    }

    /**
     * Show sell interface
     */
    private void showSellInterface(Player player, ChestShop shop) {
        String formattedPrice = EconomyManager.getInstance().format(shop.getPrice());

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Shop Info ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Owner: ").color(NamedTextColor.GRAY)
                .append(Component.text(shop.getOwnerName()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Buying: ").color(NamedTextColor.GRAY)
                .append(Component.text(shop.getItemType()).color(NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Price: ").color(NamedTextColor.GRAY)
                .append(Component.text(formattedPrice + " each").color(NamedTextColor.GOLD)));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Left-click to sell 1, Shift+Left-click for stack")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Type /cshop sell <amount> to sell specific amount")
                .color(NamedTextColor.GRAY));
    }

    /**
     * Show shop info
     */
    private void handleShopInfo(Player player, ChestShop shop) {
        if (shop.getShopType() == ChestShop.ShopType.SELLING) {
            showBuyInterface(player, shop);
        } else {
            showSellInterface(player, shop);
        }
    }

    /**
     * Prevent breaking protected shop chests
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isChest(block.getType()))
            return;

        ChestShop shop = shopManager.getShopAt(block);
        if (shop == null)
            return;

        Player player = event.getPlayer();

        // Only owner can break
        if (!shop.isOwner(player.getUniqueId()) && !player.hasPermission("vonixcore.shops.admin.remove")) {
            player.sendMessage(Component.text("You cannot break this shop! Only the owner can remove it.")
                    .color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Delete shop
        shopManager.deleteShop(shop);
        player.sendMessage(Component.text("Shop removed.").color(NamedTextColor.YELLOW));
    }

    /**
     * Create a sign on the chest
     */
    private void createShopSign(Block chestBlock, ChestShop shop) {
        // Find a suitable face for the sign
        BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
        for (BlockFace face : faces) {
            Block adjacent = chestBlock.getRelative(face);
            if (adjacent.getType() == Material.AIR) {
                adjacent.setType(Material.OAK_WALL_SIGN);
                if (adjacent.getState() instanceof Sign sign) {
                    org.bukkit.block.data.type.WallSign signData = (org.bukkit.block.data.type.WallSign) sign
                            .getBlockData();
                    signData.setFacing(face.getOppositeFace());
                    sign.setBlockData(signData);

                    // Set sign text
                    String typeText = shop.getShopType() == ChestShop.ShopType.SELLING ? "[Sell]" : "[Buy]";
                    sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0,
                            Component.text(typeText).color(NamedTextColor.DARK_BLUE));
                    sign.getSide(org.bukkit.block.sign.Side.FRONT).line(1,
                            Component.text(shop.getItemType()).color(NamedTextColor.BLACK));
                    sign.getSide(org.bukkit.block.sign.Side.FRONT).line(2,
                            Component.text(EconomyManager.getInstance().format(shop.getPrice()))
                                    .color(NamedTextColor.DARK_GREEN));
                    sign.getSide(org.bukkit.block.sign.Side.FRONT).line(3,
                            Component.text(shop.getOwnerName()).color(NamedTextColor.DARK_GRAY));
                    sign.update();
                }
                break;
            }
        }
    }

    /**
     * Check if player can create shop at location
     */
    private boolean canCreateShop(Player player, Block block) {
        // Check permission
        if (!player.hasPermission("vonixcore.shops.create")) {
            player.sendMessage(Component.text("You don't have permission to create shops!")
                    .color(NamedTextColor.RED));
            return false;
        }

        // Check if player owns/can access the chest
        if (ShopsConfig.chestShopsRequireChestAccess) {
            // TODO: Integration with protection plugins
        }

        return true;
    }

    private boolean isChest(Material material) {
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.BARREL;
    }

    /**
     * Data class for shop creation
     */
    private static class ShopCreationData {
        org.bukkit.Location chestLocation;
        ItemStack item;
        double price;
        ChestShop.ShopType shopType;
        CreationStep step;
        long timestamp;
    }

    private enum CreationStep {
        ENTER_PRICE,
        SELECT_TYPE
    }
}
