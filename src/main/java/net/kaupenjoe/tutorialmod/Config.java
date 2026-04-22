package net.kaupenjoe.tutorialmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue METEOR_MIN_DELAY_TICKS = BUILDER
            .comment("Minimum delay before the one-time meteor event starts.")
            .defineInRange("meteorMinDelayTicks", 20 * 60, 20, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue METEOR_MAX_DELAY_TICKS = BUILDER
            .comment("Maximum delay before the one-time meteor event starts.")
            .defineInRange("meteorMaxDelayTicks", 20 * 60 * 5, 20, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue METEOR_MIN_DISTANCE = BUILDER
            .comment("Minimum strike distance from the focal player.")
            .defineInRange("meteorMinDistance", 96, 48, 256);

    private static final ForgeConfigSpec.IntValue METEOR_MAX_DISTANCE = BUILDER
            .comment("Maximum strike distance from the focal player.")
            .defineInRange("meteorMaxDistance", 144, 64, 256);

    private static final ForgeConfigSpec.IntValue ALEXANDRITE_SPREAD_ATTEMPTS = BUILDER
            .comment("How many nearby blocks the alexandrite core tries to infect every random tick.")
            .defineInRange("alexandriteSpreadAttempts", 3, 1, 12);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int meteorMinDelayTicks;
    public static int meteorMaxDelayTicks;
    public static int meteorMinDistance;
    public static int meteorMaxDistance;
    public static int alexandriteSpreadAttempts;

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        meteorMinDelayTicks = METEOR_MIN_DELAY_TICKS.get();
        meteorMaxDelayTicks = Math.max(meteorMinDelayTicks, METEOR_MAX_DELAY_TICKS.get());
        meteorMinDistance = METEOR_MIN_DISTANCE.get();
        meteorMaxDistance = Math.max(meteorMinDistance, METEOR_MAX_DISTANCE.get());
        alexandriteSpreadAttempts = ALEXANDRITE_SPREAD_ATTEMPTS.get();
    }
}
