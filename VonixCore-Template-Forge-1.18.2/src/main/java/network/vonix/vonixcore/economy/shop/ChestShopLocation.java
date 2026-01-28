package network.vonix.vonixcore.economy.shop;

import net.minecraft.core.BlockPos;

public class ChestShopLocation {

    private final BlockPos pos;
    private final String itemId;

    public ChestShopLocation(BlockPos pos, String itemId) {
        this.pos = pos;
        this.itemId = itemId;
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getItemId() {
        return itemId;
    }
}
