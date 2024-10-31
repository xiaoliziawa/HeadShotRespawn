package com.prizowo.headshotrespawn;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Headshotrespawn.MOD_ID)
public class Headshotrespawn {
    public static final String MOD_ID = "headshotrespawn";

    public Headshotrespawn() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
