package com.prizowo.headshotrespawn.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.headshotrespawn";
    
    public static final KeyMapping TOGGLE_HEADSHOT_MODE = new KeyMapping(
        "key.headshotrespawn.toggle",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        CATEGORY
    );
} 