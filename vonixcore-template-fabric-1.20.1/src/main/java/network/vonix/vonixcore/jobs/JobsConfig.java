package network.vonix.vonixcore.jobs;

import network.vonix.vonixcore.config.BaseConfig;

import java.nio.file.Path;

public class JobsConfig extends BaseConfig {

    private static JobsConfig instance;

    public static JobsConfig getInstance() {
        if (instance == null) {
            instance = new JobsConfig();
        }
        return instance;
    }

    public static void init(Path configDir) {
        getInstance().load(configDir);
    }

    private JobsConfig() {
        super("jobs.yml");
    }

    @Override
    protected String getHeader() {
        return """
                # VonixCore Jobs Configuration
                # Define jobs, income, and experience rates
                """;
    }

    @Override
    protected void setDefaults() {
        // Global Settings
        setDefault("enabled", true);
        setDefault("max-jobs", 3);
        setDefault("global-income-multiplier", 1.0);
        setDefault("global-exp-multiplier", 1.0);

        // Miner Job
        setDefault("jobs.miner.name", "&7Miner");
        setDefault("jobs.miner.description", "Earn money by mining ores and stone");
        setDefault("jobs.miner.color", "GRAY");
        setDefault("jobs.miner.icon", "DIAMOND_PICKAXE");
        setDefault("jobs.miner.max-level", 100);

        setDefault("jobs.miner.actions.break.stone.income", 0.5);
        setDefault("jobs.miner.actions.break.stone.exp", 1.0);
        setDefault("jobs.miner.actions.break.coal_ore.income", 2.0);
        setDefault("jobs.miner.actions.break.coal_ore.exp", 3.0);
        setDefault("jobs.miner.actions.break.iron_ore.income", 3.0);
        setDefault("jobs.miner.actions.break.iron_ore.exp", 5.0);
        setDefault("jobs.miner.actions.break.gold_ore.income", 5.0);
        setDefault("jobs.miner.actions.break.gold_ore.exp", 8.0);
        setDefault("jobs.miner.actions.break.diamond_ore.income", 10.0);
        setDefault("jobs.miner.actions.break.diamond_ore.exp", 15.0);
        setDefault("jobs.miner.actions.break.emerald_ore.income", 12.0);
        setDefault("jobs.miner.actions.break.emerald_ore.exp", 20.0);
        setDefault("jobs.miner.actions.break.deepslate_diamond_ore.income", 12.0);
        setDefault("jobs.miner.actions.break.deepslate_diamond_ore.exp", 18.0);

        // Woodcutter Job
        setDefault("jobs.woodcutter.name", "&2Woodcutter");
        setDefault("jobs.woodcutter.description", "Earn money by chopping trees");
        setDefault("jobs.woodcutter.color", "DARK_GREEN");
        setDefault("jobs.woodcutter.icon", "DIAMOND_AXE");
        setDefault("jobs.woodcutter.max-level", 100);

        setDefault("jobs.woodcutter.actions.break.oak_log.income", 1.0);
        setDefault("jobs.woodcutter.actions.break.oak_log.exp", 2.0);
        setDefault("jobs.woodcutter.actions.break.spruce_log.income", 1.0);
        setDefault("jobs.woodcutter.actions.break.spruce_log.exp", 2.0);
        setDefault("jobs.woodcutter.actions.break.birch_log.income", 1.0);
        setDefault("jobs.woodcutter.actions.break.birch_log.exp", 2.0);
        setDefault("jobs.woodcutter.actions.break.jungle_log.income", 1.5);
        setDefault("jobs.woodcutter.actions.break.jungle_log.exp", 2.5);
        setDefault("jobs.woodcutter.actions.break.acacia_log.income", 1.5);
        setDefault("jobs.woodcutter.actions.break.acacia_log.exp", 2.5);
        setDefault("jobs.woodcutter.actions.break.dark_oak_log.income", 1.5);
        setDefault("jobs.woodcutter.actions.break.dark_oak_log.exp", 2.5);
        setDefault("jobs.woodcutter.actions.break.cherry_log.income", 2.0);
        setDefault("jobs.woodcutter.actions.break.cherry_log.exp", 3.0);

        // Farmer Job
        setDefault("jobs.farmer.name", "&aFarmer");
        setDefault("jobs.farmer.description", "Earn money by farming crops");
        setDefault("jobs.farmer.color", "GREEN");
        setDefault("jobs.farmer.icon", "DIAMOND_HOE");
        setDefault("jobs.farmer.max-level", 100);

        setDefault("jobs.farmer.actions.break.wheat.income", 1.0);
        setDefault("jobs.farmer.actions.break.wheat.exp", 1.5);
        setDefault("jobs.farmer.actions.break.carrots.income", 1.0);
        setDefault("jobs.farmer.actions.break.carrots.exp", 1.5);
        setDefault("jobs.farmer.actions.break.potatoes.income", 1.0);
        setDefault("jobs.farmer.actions.break.potatoes.exp", 1.5);
        setDefault("jobs.farmer.actions.break.beetroots.income", 1.5);
        setDefault("jobs.farmer.actions.break.beetroots.exp", 2.0);
        setDefault("jobs.farmer.actions.break.nether_wart.income", 2.0);
        setDefault("jobs.farmer.actions.break.nether_wart.exp", 3.0);

        setDefault("jobs.farmer.actions.breed.cow.income", 5.0);
        setDefault("jobs.farmer.actions.breed.cow.exp", 8.0);
        setDefault("jobs.farmer.actions.breed.pig.income", 4.0);
        setDefault("jobs.farmer.actions.breed.pig.exp", 6.0);
        setDefault("jobs.farmer.actions.breed.sheep.income", 3.0);
        setDefault("jobs.farmer.actions.breed.sheep.exp", 5.0);
        setDefault("jobs.farmer.actions.breed.chicken.income", 2.0);
        setDefault("jobs.farmer.actions.breed.chicken.exp", 3.0);

        // Hunter Job
        setDefault("jobs.hunter.name", "&cHunter");
        setDefault("jobs.hunter.description", "Earn money by killing mobs");
        setDefault("jobs.hunter.color", "RED");
        setDefault("jobs.hunter.icon", "DIAMOND_SWORD");
        setDefault("jobs.hunter.max-level", 100);

        setDefault("jobs.hunter.actions.kill.zombie.income", 2.0);
        setDefault("jobs.hunter.actions.kill.zombie.exp", 3.0);
        setDefault("jobs.hunter.actions.kill.skeleton.income", 2.5);
        setDefault("jobs.hunter.actions.kill.skeleton.exp", 4.0);
        setDefault("jobs.hunter.actions.kill.spider.income", 2.0);
        setDefault("jobs.hunter.actions.kill.spider.exp", 3.0);
        setDefault("jobs.hunter.actions.kill.creeper.income", 3.0);
        setDefault("jobs.hunter.actions.kill.creeper.exp", 5.0);
        setDefault("jobs.hunter.actions.kill.enderman.income", 5.0);
        setDefault("jobs.hunter.actions.kill.enderman.exp", 8.0);
        setDefault("jobs.hunter.actions.kill.blaze.income", 6.0);
        setDefault("jobs.hunter.actions.kill.blaze.exp", 10.0);
        setDefault("jobs.hunter.actions.kill.wither_skeleton.income", 8.0);
        setDefault("jobs.hunter.actions.kill.wither_skeleton.exp", 12.0);
        setDefault("jobs.hunter.actions.kill.warden.income", 350.0);
        setDefault("jobs.hunter.actions.kill.warden.exp", 100.0);

        // Fisherman Job
        setDefault("jobs.fisherman.name", "&bFisherman");
        setDefault("jobs.fisherman.description", "Earn money by fishing");
        setDefault("jobs.fisherman.color", "AQUA");
        setDefault("jobs.fisherman.icon", "FISHING_ROD");
        setDefault("jobs.fisherman.max-level", 100);

        setDefault("jobs.fisherman.actions.fish.cod.income", 3.0);
        setDefault("jobs.fisherman.actions.fish.cod.exp", 4.0);
        setDefault("jobs.fisherman.actions.fish.salmon.income", 4.0);
        setDefault("jobs.fisherman.actions.fish.salmon.exp", 5.0);
        setDefault("jobs.fisherman.actions.fish.tropical_fish.income", 5.0);
        setDefault("jobs.fisherman.actions.fish.tropical_fish.exp", 7.0);
        setDefault("jobs.fisherman.actions.fish.pufferfish.income", 6.0);
        setDefault("jobs.fisherman.actions.fish.pufferfish.exp", 8.0);
        setDefault("jobs.fisherman.actions.fish.*.income", 2.0);
        setDefault("jobs.fisherman.actions.fish.*.exp", 3.0);

        // Builder Job
        setDefault("jobs.builder.name", "&6Builder");
        setDefault("jobs.builder.description", "Earn money by placing blocks");
        setDefault("jobs.builder.color", "GOLD");
        setDefault("jobs.builder.icon", "BRICKS");
        setDefault("jobs.builder.max-level", 100);

        setDefault("jobs.builder.actions.place.*.income", 0.25);
        setDefault("jobs.builder.actions.place.*.exp", 0.5);
        setDefault("jobs.builder.actions.place.bricks.income", 1.0);
        setDefault("jobs.builder.actions.place.bricks.exp", 1.5);
        setDefault("jobs.builder.actions.place.stone_bricks.income", 0.75);
        setDefault("jobs.builder.actions.place.stone_bricks.exp", 1.0);

        // Crafter Job
        setDefault("jobs.crafter.name", "&eCrafter");
        setDefault("jobs.crafter.description", "Earn money by crafting items");
        setDefault("jobs.crafter.color", "YELLOW");
        setDefault("jobs.crafter.icon", "CRAFTING_TABLE");
        setDefault("jobs.crafter.max-level", 100);

        setDefault("jobs.crafter.actions.craft.iron_pickaxe.income", 5.0);
        setDefault("jobs.crafter.actions.craft.iron_pickaxe.exp", 8.0);
        setDefault("jobs.crafter.actions.craft.diamond_pickaxe.income", 15.0);
        setDefault("jobs.crafter.actions.craft.diamond_pickaxe.exp", 20.0);
        setDefault("jobs.crafter.actions.craft.iron_sword.income", 4.0);
        setDefault("jobs.crafter.actions.craft.iron_sword.exp", 6.0);
        setDefault("jobs.crafter.actions.craft.diamond_sword.income", 12.0);
        setDefault("jobs.crafter.actions.craft.diamond_sword.exp", 18.0);
    }
}
