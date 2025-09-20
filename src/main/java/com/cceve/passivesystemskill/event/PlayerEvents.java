package com.cceve.passivesystemskill.event;

import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.pss_main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = pss_main.MODID)
public class PlayerEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 延迟 1 tick 执行
            player.getServer().execute(() -> {
                player.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(vars -> {

                });
            });
        }
    }
}
