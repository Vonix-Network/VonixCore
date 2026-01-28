package network.vonix.vonixcore.economy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.economy.EconomyManager;

public class ShopEventListener {

    @SubscribeEvent
    public void onSignChange(BlockEvent.EntityPlaceEvent event) {
        if (event.getPlacedBlock().getBlock().getRegistryName().toString().contains("sign")) {
            if (event.getPlacedBlock().hasBlockEntity()) {
                if (event.getWorld().getBlockEntity(event.getPos()) instanceof SignBlockEntity) {
                    SignBlockEntity sign = (SignBlockEntity) event.getWorld().getBlockEntity(event.getPos());
                    // Logic for creating a shop will go here
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getWorld().getBlockState(event.getPos());
        if (state.getBlock().getRegistryName().toString().contains("sign")) {
            if (event.getWorld().getBlockEntity(event.getPos()) instanceof SignBlockEntity) {
                SignBlockEntity sign = (SignBlockEntity) event.getWorld().getBlockEntity(event.getPos());
                // Logic for interacting with a shop will go here
            }
        }
    }
}
