package network.vonix.vonixcore.economy.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import network.vonix.vonixcore.economy.ShopManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * Custom shop menu that intercepts clicks for shop functionality.
 * Compatible with vanilla clients by using standard GENERIC_9x6.
 * Forge 1.18.2 compatible version.
 */
public class ShopMenu extends ChestMenu {

    private final ShopType shopType;
    private final Player player;
    private int currentPage = 0;
    private Consumer<ShopClickEvent> clickHandler;

    public enum ShopType {
        ADMIN_SHOP,
        PLAYER_MARKET,
        CATEGORY_SELECT
    }

    public ShopMenu(int containerId, Inventory playerInventory, ShopType type) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), 6);
        this.shopType = type;
        this.player = playerInventory.player;
    }

    public ShopMenu(int containerId, Inventory playerInventory, Container container, ShopType type) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
        this.shopType = type;
        this.player = playerInventory.player;
    }

    /**
     * Override clicked to intercept shop interactions
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Only handle clicks in the container (0-53), not player inventory
        if (slotId >= 0 && slotId < 54) {
            // Prevent item movement
            if (clickHandler != null && player instanceof ServerPlayer serverPlayer) {
                ItemStack clickedItem = slotId < getContainer().getContainerSize()
                        ? getContainer().getItem(slotId)
                        : ItemStack.EMPTY;

                ShopClickEvent event = new ShopClickEvent(
                        serverPlayer,
                        slotId,
                        button,
                        clickType,
                        clickedItem,
                        currentPage);
                clickHandler.accept(event);
            }
            // Don't call super - prevents item movement
            return;
        }

        // Allow normal player inventory interaction
        super.clicked(slotId, button, clickType, player);
    }

    /**
     * Set the click handler for this menu
     */
    public void setClickHandler(Consumer<ShopClickEvent> handler) {
        this.clickHandler = handler;
    }

    /**
     * Set current page for pagination
     */
    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public ShopType getShopType() {
        return shopType;
    }

    /**
     * Populate the menu with admin shop items
     */
    public void populateAdminShop(int page) {
        Container container = getContainer();

        // Clear container
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        ShopManager.getInstance().getAllAdminItems().thenAccept(items -> {
            if (player.getServer() == null) return;
            player.getServer().execute(() -> {
                int itemsPerPage = 45; // 5 rows of items
                int startIndex = page * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, items.size());

                // Populate items
                for (int i = startIndex; i < endIndex; i++) {
                    int slot = i - startIndex;
                    ShopManager.AdminShopItem shopItem = items.get(i);

                    ItemStack displayStack = ItemUtils.createItemFromId(shopItem.itemId());
                    if (!displayStack.isEmpty()) {
                        // Add price lore
                        ItemUtils.addPriceLore(displayStack, shopItem.buyPrice(), shopItem.sellPrice());
                        container.setItem(slot, displayStack);
                    }
                }

                // Navigation row (bottom row, slots 45-53)
                if (page > 0) {
                    ItemStack prevPage = new ItemStack(Items.ARROW);
                    prevPage.setHoverName(new TextComponent("§e« Previous Page"));
                    container.setItem(45, prevPage);
                }

                // Info item
                ItemStack info = new ItemStack(Items.BOOK);
                info.setHoverName(new TextComponent("§6Admin Shop - Page " + (page + 1)));
                container.setItem(49, info);

                if (endIndex < items.size()) {
                    ItemStack nextPage = new ItemStack(Items.ARROW);
                    nextPage.setHoverName(new TextComponent("§eNext Page »"));
                    container.setItem(53, nextPage);
                }
            });
        });

        this.currentPage = page;
    }

    /**
     * Populate the menu with player market listings
     */
    public void populatePlayerMarket(int page) {
        Container container = getContainer();

        // Clear container
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        ShopManager.getInstance().getAllListings().thenAccept(listings -> {
            if (player.getServer() == null) return;
            player.getServer().execute(() -> {
                int itemsPerPage = 45;
                int startIndex = page * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, listings.size());

                // Populate listings
                for (int i = startIndex; i < endIndex; i++) {
                    int slot = i - startIndex;
                    ShopManager.PlayerListing listing = listings.get(i);

                    ItemStack displayStack = ItemUtils.createItemFromId(listing.itemId());
                    if (!displayStack.isEmpty()) {
                        displayStack.setCount(listing.quantity());
                        ItemUtils.addListingLore(displayStack, listing.price(), listing.seller());
                        container.setItem(slot, displayStack);
                    }
                }

                // Navigation
                if (page > 0) {
                    ItemStack prevPage = new ItemStack(Items.ARROW);
                    prevPage.setHoverName(new TextComponent("§e« Previous Page"));
                    container.setItem(45, prevPage);
                }

                ItemStack info = new ItemStack(Items.EMERALD);
                info.setHoverName(new TextComponent("§aPlayer Market - Page " + (page + 1)));
                container.setItem(49, info);

                if (endIndex < listings.size()) {
                    ItemStack nextPage = new ItemStack(Items.ARROW);
                    nextPage.setHoverName(new TextComponent("§eNext Page »"));
                    container.setItem(53, nextPage);
                }
            });
        });

        this.currentPage = page;
    }

    /**
     * Event data for shop clicks
     */
    public record ShopClickEvent(
            ServerPlayer player,
            int slotId,
            int button,
            ClickType clickType,
            ItemStack clickedItem,
            int currentPage) {
        public boolean isLeftClick() {
            return button == 0 && clickType == ClickType.PICKUP;
        }

        public boolean isRightClick() {
            return button == 1 && clickType == ClickType.PICKUP;
        }

        public boolean isShiftClick() {
            return clickType == ClickType.QUICK_MOVE;
        }
    }
}
