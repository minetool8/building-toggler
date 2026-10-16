package com.example.globalbuildingtoggle;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(GlobalBuildingToggle.MOD_ID)
public class GlobalBuildingToggle {
    public static final String MOD_ID = "globalbuildingtoggle";

    public GlobalBuildingToggle(IEventBus modEventBus) {
        // All logic runs on the game bus and is server-authoritative.
        NeoForge.EVENT_BUS.register(new BuildingEvents());
    }
}
