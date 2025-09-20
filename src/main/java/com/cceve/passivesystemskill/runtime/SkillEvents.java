package com.cceve.passivesystemskill.runtime;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.liandong.iron_spell.ISSAttribute;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;

@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkillEvents {

    private SkillEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp)
            applyNextTick(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp)
            applyNextTick(sp);
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp)
            applyNextTick(sp);
    }

    private static void applyNextTick(ServerPlayer sp) {
        // 放到服务器执行队列下一帧执行
        sp.getServer().execute(() -> SkillAttributeApplier.applyAllSafe(sp));
        sp.getServer().execute(() -> ISSAttribute.applyAllSafe(sp));
    }
}
