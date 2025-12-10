package network.vonix.vonixcore.shops;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.shops.chest.ChestShop;
import network.vonix.vonixcore.shops.chest.ChestShopManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for all shop-related commands.
 */
public class ShopsCommands implements CommandExecutor, TabCompleter {

    private final VonixCore plugin;
    private final ShopsManager shopsManager;

    public ShopsCommands(VonixCore plugin, ShopsManager shopsManager) {
        this.plugin = plugin;
        this.shopsManager = shopsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "shop" -> handleShopCommand(sender, args);
            case "cshop" -> handleChestShopCommand(sender, args);
            case "market", "pshop" -> handleMarketCommand(sender, args);
            case "shopadmin" -> handleShopAdminCommand(sender, args);
        }

        return true;
    }

    // === /shop - Server GUI Shop ===
    private void handleShopCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
            return;
        }

        if (!ShopsConfig.guiShopEnabled) {
            player.sendMessage(Component.text("Server shop is not enabled!").color(NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("vonixcore.shop")) {
            player.sendMessage(Component.text("You don't have permission to use the shop!").color(NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            shopsManager.getServerShopManager().openMainMenu(player);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "sellall" -> {
                if (ShopsConfig.guiShopSellAllEnabled) {
                    // Trigger sell all through the manager
                    shopsManager.getServerShopManager().openMainMenu(player);
                    player.sendMessage(
                            Component.text("Use the Sell All button in the shop menu!").color(NamedTextColor.YELLOW));
                }
            }
            default -> shopsManager.getServerShopManager().openMainMenu(player);
        }
    }

    // === /cshop - Chest Shop Commands ===
    private void handleChestShopCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
            return;
        }

        if (!ShopsConfig.chestShopsEnabled) {
            player.sendMessage(Component.text("Chest shops are not enabled!").color(NamedTextColor.RED));
            return;
        }

        ChestShopManager csm = shopsManager.getChestShopManager();
        if (csm == null)
            return;

        if (args.length == 0) {
            sendChestShopHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCShopCreate(player, args);
            case "remove", "delete" -> handleCShopRemove(player);
            case "info" -> handleCShopInfo(player);
            case "setprice", "price" -> handleCShopSetPrice(player, args);
            case "buy" -> handleCShopBuy(player, args);
            case "sell" -> handleCShopSell(player, args);
            case "find", "search" -> handleCShopFind(player, args);
            case "staff" -> handleCShopStaff(player, args);
            case "list" -> handleCShopList(player);
            default -> sendChestShopHelp(player);
        }
    }

    private void sendChestShopHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Chest Shop Commands ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/cshop create <price> [buy|sell]").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Create shop").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cshop remove").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Remove shop you're looking at").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cshop info").color(NamedTextColor.YELLOW)
                .append(Component.text(" - View shop info").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cshop setprice <price>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Change shop price").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cshop buy <amount>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Buy from shop").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cshop sell <amount>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Sell to shop").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cshop find <item>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Find shops selling item").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cshop list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - List your shops").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());
    }

    private void handleCShopCreate(Player player, String[] args) {
        if (!player.hasPermission("vonixcore.shops.create")) {
            player.sendMessage(Component.text("You don't have permission to create shops!").color(NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /cshop create <price> [buy|sell]").color(NamedTextColor.RED));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid price!").color(NamedTextColor.RED));
            return;
        }

        ChestShop.ShopType type = ChestShop.ShopType.SELLING;
        if (args.length >= 3) {
            if (args[2].equalsIgnoreCase("buy") || args[2].equalsIgnoreCase("buying")) {
                type = ChestShop.ShopType.BUYING;
            }
        }

        // Get targeted block
        Block target = player.getTargetBlockExact(5);
        if (target == null || !isChest(target.getType())) {
            player.sendMessage(Component.text("Look at a chest to create a shop!").color(NamedTextColor.RED));
            return;
        }

        // Get item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(Component.text("Hold the item you want to sell!").color(NamedTextColor.RED));
            return;
        }

        ChestShopManager csm = shopsManager.getChestShopManager();
        ChestShop shop = csm.createShop(player, target.getLocation(), item, price, type);

        if (shop != null) {
            player.sendMessage(Component.text("Shop created successfully!").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(
                    Component.text("Failed to create shop. Check limits and permissions.").color(NamedTextColor.RED));
        }
    }

    private void handleCShopRemove(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Look at a shop to remove it!").color(NamedTextColor.RED));
            return;
        }

        ChestShopManager csm = shopsManager.getChestShopManager();
        ChestShop shop = csm.getShopAt(target);
        if (shop == null) {
            player.sendMessage(Component.text("No shop at that location!").color(NamedTextColor.RED));
            return;
        }

        if (!shop.isOwner(player.getUniqueId()) && !player.hasPermission("vonixcore.shops.admin.remove")) {
            player.sendMessage(Component.text("You don't own this shop!").color(NamedTextColor.RED));
            return;
        }

        csm.deleteShop(shop);
        player.sendMessage(Component.text("Shop removed!").color(NamedTextColor.GREEN));
    }

    private void handleCShopInfo(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Look at a shop!").color(NamedTextColor.RED));
            return;
        }

        ChestShopManager csm = shopsManager.getChestShopManager();
        ChestShop shop = csm.getShopAt(target);
        if (shop == null) {
            player.sendMessage(Component.text("No shop at that location!").color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Shop Info ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Owner: ").color(NamedTextColor.GRAY)
                .append(Component.text(shop.getOwnerName()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Item: ").color(NamedTextColor.GRAY)
                .append(Component.text(shop.getItemType()).color(NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Price: ").color(NamedTextColor.GRAY)
                .append(Component.text(EconomyManager.getInstance().format(shop.getPrice()))
                        .color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Type: ").color(NamedTextColor.GRAY)
                .append(Component.text(shop.getShopType().name()).color(NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Stock: ").color(NamedTextColor.GRAY)
                .append(Component.text(shop.isUnlimited() ? "Unlimited" : String.valueOf(shop.getStock()))
                        .color(NamedTextColor.GREEN)));
        if (shop.isAdmin()) {
            player.sendMessage(Component.text("Admin Shop").color(NamedTextColor.RED));
        }
        player.sendMessage(Component.empty());
    }

    private void handleCShopSetPrice(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /cshop setprice <price>").color(NamedTextColor.RED));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Look at your shop!").color(NamedTextColor.RED));
            return;
        }

        ChestShopManager csm = shopsManager.getChestShopManager();
        ChestShop shop = csm.getShopAt(target);
        if (shop == null || (!shop.isOwner(player.getUniqueId()) && !player.hasPermission("vonixcore.shops.admin"))) {
            player.sendMessage(Component.text("You don't own this shop!").color(NamedTextColor.RED));
            return;
        }

        try {
            double price = Double.parseDouble(args[1]);
            shop.setPrice(price);
            player.sendMessage(Component.text("Price updated to " + EconomyManager.getInstance().format(price))
                    .color(NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid price!").color(NamedTextColor.RED));
        }
    }

    private void handleCShopBuy(Player player, String[] args) {
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid amount!").color(NamedTextColor.RED));
                return;
            }
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Look at a shop!").color(NamedTextColor.RED));
            return;
        }

        ChestShopManager csm = shopsManager.getChestShopManager();
        ChestShop shop = csm.getShopAt(target);
        if (shop == null) {
            player.sendMessage(Component.text("No shop at that location!").color(NamedTextColor.RED));
            return;
        }

        if (shop.getShopType() != ChestShop.ShopType.SELLING) {
            player.sendMessage(Component.text("This shop doesn't sell items!").color(NamedTextColor.RED));
            return;
        }

        if (csm.processPurchase(player, shop, amount)) {
            player.sendMessage(Component.text("Purchase successful!").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(
                    Component.text("Purchase failed! Check your balance and shop stock.").color(NamedTextColor.RED));
        }
    }

    private void handleCShopSell(Player player, String[] args) {
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid amount!").color(NamedTextColor.RED));
                return;
            }
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Look at a shop!").color(NamedTextColor.RED));
            return;
        }

        ChestShopManager csm = shopsManager.getChestShopManager();
        ChestShop shop = csm.getShopAt(target);
        if (shop == null) {
            player.sendMessage(Component.text("No shop at that location!").color(NamedTextColor.RED));
            return;
        }

        if (shop.getShopType() != ChestShop.ShopType.BUYING) {
            player.sendMessage(Component.text("This shop doesn't buy items!").color(NamedTextColor.RED));
            return;
        }

        if (csm.processSale(player, shop, amount)) {
            player.sendMessage(Component.text("Sale successful!").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(
                    Component.text("Sale failed! Check your inventory and shop funds.").color(NamedTextColor.RED));
        }
    }

    private void handleCShopFind(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /cshop find <item>").color(NamedTextColor.RED));
            return;
        }

        String itemName = args[1].toUpperCase();
        ChestShopManager csm = shopsManager.getChestShopManager();
        var shops = csm.findShops(itemName, ShopsConfig.chestShopsFindDistance, player.getLocation());

        if (shops.isEmpty()) {
            player.sendMessage(
                    Component.text("No shops found selling " + itemName + " nearby!").color(NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("Found " + shops.size() + " shops:").color(NamedTextColor.GREEN));
        for (var shop : shops.stream().limit(10).toList()) {
            var loc = shop.getLocation();
            player.sendMessage(Component.text("  - " + shop.getOwnerName() + ": ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(EconomyManager.getInstance().format(shop.getPrice()))
                            .color(NamedTextColor.GOLD))
                    .append(Component.text(" at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ())
                            .color(NamedTextColor.GRAY)));
        }
    }

    private void handleCShopStaff(Player player, String[] args) {
        // Staff management - to be implemented
        player.sendMessage(Component.text("Staff management coming soon!").color(NamedTextColor.YELLOW));
    }

    private void handleCShopList(Player player) {
        ChestShopManager csm = shopsManager.getChestShopManager();
        var shops = csm.getPlayerShops(player.getUniqueId());

        if (shops.isEmpty()) {
            player.sendMessage(Component.text("You don't have any shops!").color(NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("Your shops (" + shops.size() + "):").color(NamedTextColor.GREEN));
        for (var shop : shops) {
            var loc = shop.getLocation();
            player.sendMessage(Component.text("  - " + shop.getItemType() + ": ")
                    .color(NamedTextColor.AQUA)
                    .append(Component.text(EconomyManager.getInstance().format(shop.getPrice()))
                            .color(NamedTextColor.GOLD))
                    .append(Component.text(" at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ())
                            .color(NamedTextColor.GRAY)));
        }
    }

    // === /market, /pshop - Player Market ===
    private void handleMarketCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
            return;
        }

        if (!ShopsConfig.playerMarketEnabled) {
            player.sendMessage(Component.text("Player market is not enabled!").color(NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("vonixcore.market")) {
            player.sendMessage(
                    Component.text("You don't have permission to use the market!").color(NamedTextColor.RED));
            return;
        }

        var pmm = shopsManager.getPlayerMarketManager();
        if (pmm == null)
            return;

        if (args.length == 0) {
            pmm.openBrowseMenu(player, 0, null);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "browse", "list" -> pmm.openBrowseMenu(player, 0, null);
            case "my", "mylistings" -> pmm.openMyListings(player);
            case "create", "sell" -> handleMarketCreate(player, args);
            case "search" -> {
                if (args.length >= 2) {
                    pmm.openBrowseMenu(player, 0, args[1]);
                } else {
                    player.sendMessage(Component.text("Usage: /market search <item>").color(NamedTextColor.RED));
                }
            }
            case "collect" -> {
                double collected = pmm.collectEarnings(player);
                if (collected > 0) {
                    player.sendMessage(Component.text("Collected " + EconomyManager.getInstance().format(collected))
                            .color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("No earnings to collect!").color(NamedTextColor.YELLOW));
                }
            }
            default -> {
                player.sendMessage(Component.text("Usage: /market [browse|my|create|search|collect]")
                        .color(NamedTextColor.YELLOW));
            }
        }
    }

    private void handleMarketCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /market create <price>").color(NamedTextColor.RED));
            player.sendMessage(Component.text("Hold the items you want to list!").color(NamedTextColor.GRAY));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid price!").color(NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(Component.text("Hold the items you want to list!").color(NamedTextColor.RED));
            return;
        }

        shopsManager.getPlayerMarketManager().createListing(player, item, price);
    }

    // === /shopadmin - Admin Commands ===
    private void handleShopAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vonixcore.shops.admin")) {
            sender.sendMessage(Component.text("You don't have permission!").color(NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /shopadmin <reload|stats>").color(NamedTextColor.YELLOW));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                shopsManager.reload();
                sender.sendMessage(Component.text("Shop configurations reloaded!").color(NamedTextColor.GREEN));
            }
            case "stats" -> {
                ChestShopManager csm = shopsManager.getChestShopManager();
                int chestShops = csm != null ? csm.getTotalShopCount() : 0;
                sender.sendMessage(Component.text("=== Shop Statistics ===").color(NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Chest Shops: " + chestShops).color(NamedTextColor.GRAY));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        if (args.length == 1) {
            switch (cmdName) {
                case "cshop" -> completions
                        .addAll(Arrays.asList("create", "remove", "info", "setprice", "buy", "sell", "find", "list"));
                case "market", "pshop" ->
                    completions.addAll(Arrays.asList("browse", "my", "create", "search", "collect"));
                case "shopadmin" -> completions.addAll(Arrays.asList("reload", "stats"));
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private boolean isChest(Material material) {
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.BARREL;
    }
}
