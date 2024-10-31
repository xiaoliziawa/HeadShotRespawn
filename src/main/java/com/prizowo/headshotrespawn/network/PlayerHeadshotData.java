package com.prizowo.headshotrespawn.network;

import java.util.HashMap;
import java.util.UUID;

public class PlayerHeadshotData {
    private static final HashMap<UUID, Integer> playerModes = new HashMap<>();

    public static void setPlayerMode(UUID playerId, int mode) {
        playerModes.put(playerId, mode);
    }

    public static int getPlayerMode(UUID playerId) {
        return playerModes.getOrDefault(playerId, 0);
    }
} 