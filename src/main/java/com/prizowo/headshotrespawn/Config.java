package com.prizowo.headshotrespawn;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue HEADSHOT_DAMAGE_MULTIPLIER_WITHOUT_HELMET;
    public static final ForgeConfigSpec.DoubleValue HEADSHOT_DAMAGE_MULTIPLIER_WITH_HELMET;
    public static final ForgeConfigSpec.DoubleValue MAX_HEADSHOT_DAMAGE;

    static {
        BUILDER.push("Headshot damage configuration");

        HEADSHOT_DAMAGE_MULTIPLIER_WITHOUT_HELMET = BUILDER
            .comment("Headshot damage multiplier without helmet")
            .defineInRange("noHelmetMultiplier", 2.0, 1.0, 10.0);

        HEADSHOT_DAMAGE_MULTIPLIER_WITH_HELMET = BUILDER
            .comment("Headshot damage multiplier with helmet (actual damage will be capped at 75% of no helmet damage)")
            .defineInRange("withHelmetMultiplier", 1.5, 1.0, 10.0);

        MAX_HEADSHOT_DAMAGE = BUILDER
            .comment("Maximum headshot damage limit")
            .defineInRange("maxHeadshotDamage", 40.0, 1.0, 100.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
} 