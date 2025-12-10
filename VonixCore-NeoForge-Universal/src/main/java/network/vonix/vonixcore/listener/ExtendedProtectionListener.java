package network.vonix.vonixcore.listener;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ProtectionConfig;
import network.vonix.vonixcore.consumer.Consumer;

/**
 * Extended protection event listener for additional CoreProtect-style logging.
 * Logs containers, entity kills, interactions, chat, commands, and signs.
 */
@EventBusSubscriber(modid = VonixCore.MODID)
public class ExtendedProtectionListener {

    // Action constants
    private static final int ACTION_KILL = 0;
    private static final int ACTION_REMOVE = 0;
    private static final int ACTION_ADD = 1;

    /**
     * Log entity kills by players.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!ProtectionConfig.CONFIG.enabled.get())
            return;
        if (!ProtectionConfig.CONFIG.logEntityKills.getAsBoolean())
            return;

        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide())
            return;

        // Get killer
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player))
            return;

        String user = player.getName().getString();
        String world = level.dimension().location().toString();
        BlockPos pos = entity.blockPosition();
        long time = System.currentTimeMillis() / 1000L;
        String entityType = getEntityId(entity);

        Consumer.getInstance().queueEntry(new EntityLogEntry(
                time, user, world,
                pos.getX(), pos.getY(), pos.getZ(),
                entityType, ACTION_KILL));
    }

    /**
     * Log player interactions with blocks.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!ProtectionConfig.CONFIG.enabled.get())
            return;
        if (!ProtectionConfig.CONFIG.logPlayerInteractions.getAsBoolean())
            return;
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        // Don't log if player is inspecting
        if (network.vonix.vonixcore.command.ProtectionCommands.isInspecting(player.getUUID())) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        String blockType = getBlockId(state);

        // Only log interactive blocks (doors, buttons, levers, etc.)
        if (!isInteractiveBlock(blockType))
            return;

        String user = player.getName().getString();
        String world = event.getLevel().dimension().location().toString();
        long time = System.currentTimeMillis() / 1000L;

        Consumer.getInstance().queueEntry(new InteractionLogEntry(
                time, user, world,
                pos.getX(), pos.getY(), pos.getZ(),
                blockType));
    }

    /**
     * Log chat messages.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChat(ServerChatEvent event) {
        if (!ProtectionConfig.CONFIG.enabled.get())
            return;
        if (!ProtectionConfig.CONFIG.logChat.getAsBoolean())
            return;

        ServerPlayer player = event.getPlayer();
        String user = player.getName().getString();
        String world = player.level().dimension().location().toString();
        BlockPos pos = player.blockPosition();
        long time = System.currentTimeMillis() / 1000L;
        String message = event.getRawText();

        Consumer.getInstance().queueEntry(new ChatLogEntry(
                time, user, world,
                pos.getX(), pos.getY(), pos.getZ(),
                message));
    }

    /**
     * Log commands.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCommand(CommandEvent event) {
        if (!ProtectionConfig.CONFIG.enabled.get())
            return;
        if (!ProtectionConfig.CONFIG.logCommands.getAsBoolean())
            return;

        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String user = player.getName().getString();
        String world = player.level().dimension().location().toString();
        BlockPos pos = player.blockPosition();
        long time = System.currentTimeMillis() / 1000L;
        String command = "/" + event.getParseResults().getReader().getString();

        // Don't log sensitive commands
        if (command.toLowerCase().contains("password") ||
                command.toLowerCase().contains("login") ||
                command.toLowerCase().contains("register")) {
            return;
        }

        Consumer.getInstance().queueEntry(new CommandLogEntry(
                time, user, world,
                pos.getX(), pos.getY(), pos.getZ(),
                command));
    }

    /**
     * Log sign text changes.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSignChange(BlockEvent.EntityPlaceEvent event) {
        if (!ProtectionConfig.CONFIG.enabled.get())
            return;
        if (!ProtectionConfig.CONFIG.logSigns.getAsBoolean())
            return;
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof SignBlockEntity sign))
            return;

        String user = player.getName().getString();
        String world = getWorldName(event.getLevel());
        long time = System.currentTimeMillis() / 1000L;

        // Get sign text (delayed to allow text to be set)
        player.getServer().execute(() -> {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                String line = sign.getFrontText().getMessage(i, false).getString();
                if (!line.isEmpty()) {
                    if (text.length() > 0)
                        text.append("|");
                    text.append(line);
                }
            }

            if (text.length() > 0) {
                Consumer.getInstance().queueEntry(new SignLogEntry(
                        time, user, world,
                        pos.getX(), pos.getY(), pos.getZ(),
                        text.toString()));
            }
        });
    }

    // Helper methods

    private static String getWorldName(net.minecraft.world.level.LevelAccessor level) {
        if (level instanceof Level l) {
            return l.dimension().location().toString();
        }
        return "unknown";
    }

    private static String getBlockId(BlockState state) {
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key != null ? key.toString() : "minecraft:air";
    }

    private static String getEntityId(Entity entity) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null ? key.toString() : "unknown";
    }

    private static boolean isInteractiveBlock(String blockType) {
        return blockType.contains("door") ||
                blockType.contains("button") ||
                blockType.contains("lever") ||
                blockType.contains("gate") ||
                blockType.contains("trapdoor") ||
                blockType.contains("chest") ||
                blockType.contains("furnace") ||
                blockType.contains("anvil") ||
                blockType.contains("crafting") ||
                blockType.contains("barrel") ||
                blockType.contains("shulker") ||
                blockType.contains("hopper") ||
                blockType.contains("dispenser") ||
                blockType.contains("dropper") ||
                blockType.contains("brewing") ||
                blockType.contains("enchant");
    }

    // Queue entry classes

    /**
     * Entity kill log entry.
     */
    public static class EntityLogEntry implements Consumer.QueueEntry {
        private final long time;
        private final String user;
        private final String world;
        private final int x, y, z;
        private final String entityType;
        private final int action;

        public EntityLogEntry(long time, String user, String world, int x, int y, int z,
                String entityType, int action) {
            this.time = time;
            this.user = user;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.entityType = entityType;
            this.action = action;
        }

        @Override
        public void execute(java.sql.Connection conn) throws java.sql.SQLException {
            String sql = "INSERT INTO vp_entity (time, user, world, x, y, z, type, action) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, time);
                stmt.setString(2, user);
                stmt.setString(3, world);
                stmt.setInt(4, x);
                stmt.setInt(5, y);
                stmt.setInt(6, z);
                stmt.setString(7, entityType);
                stmt.setInt(8, action);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Interaction log entry.
     */
    public static class InteractionLogEntry implements Consumer.QueueEntry {
        private final long time;
        private final String user;
        private final String world;
        private final int x, y, z;
        private final String blockType;

        public InteractionLogEntry(long time, String user, String world, int x, int y, int z,
                String blockType) {
            this.time = time;
            this.user = user;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockType = blockType;
        }

        @Override
        public void execute(java.sql.Connection conn) throws java.sql.SQLException {
            String sql = "INSERT INTO vp_interaction (time, user, world, x, y, z, type) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, time);
                stmt.setString(2, user);
                stmt.setString(3, world);
                stmt.setInt(4, x);
                stmt.setInt(5, y);
                stmt.setInt(6, z);
                stmt.setString(7, blockType);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Chat log entry.
     */
    public static class ChatLogEntry implements Consumer.QueueEntry {
        private final long time;
        private final String user;
        private final String world;
        private final int x, y, z;
        private final String message;

        public ChatLogEntry(long time, String user, String world, int x, int y, int z, String message) {
            this.time = time;
            this.user = user;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.message = message;
        }

        @Override
        public void execute(java.sql.Connection conn) throws java.sql.SQLException {
            String sql = "INSERT INTO vp_chat (time, user, world, x, y, z, message) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, time);
                stmt.setString(2, user);
                stmt.setString(3, world);
                stmt.setInt(4, x);
                stmt.setInt(5, y);
                stmt.setInt(6, z);
                stmt.setString(7, message);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Command log entry.
     */
    public static class CommandLogEntry implements Consumer.QueueEntry {
        private final long time;
        private final String user;
        private final String world;
        private final int x, y, z;
        private final String command;

        public CommandLogEntry(long time, String user, String world, int x, int y, int z, String command) {
            this.time = time;
            this.user = user;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.command = command;
        }

        @Override
        public void execute(java.sql.Connection conn) throws java.sql.SQLException {
            String sql = "INSERT INTO vp_command (time, user, world, x, y, z, command) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, time);
                stmt.setString(2, user);
                stmt.setString(3, world);
                stmt.setInt(4, x);
                stmt.setInt(5, y);
                stmt.setInt(6, z);
                stmt.setString(7, command);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Sign log entry.
     */
    public static class SignLogEntry implements Consumer.QueueEntry {
        private final long time;
        private final String user;
        private final String world;
        private final int x, y, z;
        private final String text;

        public SignLogEntry(long time, String user, String world, int x, int y, int z, String text) {
            this.time = time;
            this.user = user;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.text = text;
        }

        @Override
        public void execute(java.sql.Connection conn) throws java.sql.SQLException {
            String sql = "INSERT INTO vp_sign (time, user, world, x, y, z, text) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, time);
                stmt.setString(2, user);
                stmt.setString(3, world);
                stmt.setInt(4, x);
                stmt.setInt(5, y);
                stmt.setInt(6, z);
                stmt.setString(7, text);
                stmt.executeUpdate();
            }
        }
    }
}
