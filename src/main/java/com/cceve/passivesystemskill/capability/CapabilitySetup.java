package com.cceve.passivesystemskill.capability;

import com.cceve.passivesystemskill.pss_main;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CapabilitySetup {

    private CapabilitySetup() {
    }

    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent e) {
        e.register(PlayerVariables.class);
    }
}
