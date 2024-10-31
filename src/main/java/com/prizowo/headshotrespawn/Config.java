package com.prizowo.headshotrespawn;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue HEADSHOT_DAMAGE_MULTIPLIER_WITHOUT_HELMET;
    public static final ForgeConfigSpec.DoubleValue HEADSHOT_DAMAGE_MULTIPLIER_WITH_HELMET;
    public static final ForgeConfigSpec.IntValue STREAK_TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.DoubleValue SPECIAL_DROP_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_STREAK_REWARDS;
    public static final ForgeConfigSpec.DoubleValue SPECIAL_EFFECT_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPECIAL_EFFECTS;

    static {
        BUILDER.push("Headshot Configuration");

        HEADSHOT_DAMAGE_MULTIPLIER_WITHOUT_HELMET = BUILDER
            .comment("Headshot damage multiplier when not wearing a helmet")
            .defineInRange("noHelmetMultiplier", 2.0, 1.0, 10.0);

        HEADSHOT_DAMAGE_MULTIPLIER_WITH_HELMET = BUILDER
            .comment("Headshot damage multiplier when wearing a helmet")
            .defineInRange("withHelmetMultiplier", 1.5, 1.0, 10.0);

        STREAK_TIMEOUT_SECONDS = BUILDER
            .comment("Time in seconds before streak count resets")
            .defineInRange("streakTimeout", 10, 1, 60);

        SPECIAL_DROP_CHANCE = BUILDER
            .comment("Chance for special items to drop")
            .defineInRange("specialDropChance", 0.05, 0.0, 1.0);

        ENABLE_STREAK_REWARDS = BUILDER
            .comment("Enable the headshot streak reward system")
            .define("enableStreakRewards", true);

        SPECIAL_EFFECT_CHANCE = BUILDER
            .comment("Chance for special effects to trigger")
            .defineInRange("specialEffectChance", 0.1, 0.0, 1.0);

        ENABLE_SPECIAL_EFFECTS = BUILDER
            .comment("Enable special effects system")
            .define("enableSpecialEffects", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
} 