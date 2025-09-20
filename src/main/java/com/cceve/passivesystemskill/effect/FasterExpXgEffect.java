package com.cceve.passivesystemskill.effect;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.registry.ModEffects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Fasterexpxg：每一级 +10%（0.1 * (amp+1)），获得/结束都会更新；复活强制清零 */
public class FasterExpXgEffect extends MobEffect {

    public FasterExpXgEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x33FF99); // 绿色增益
    }

    @Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Hooks {

        /** 获得/覆盖时（例如重新施加更高等级） */
        @SubscribeEvent
        public static void onEffectAdded(MobEffectEvent.Added e) {
            if (!(e.getEntity() instanceof ServerPlayer sp))
                return;
            if (e.getEffectInstance() == null)
                return;
            if (e.getEffectInstance().getEffect() != ModEffects.FASTEREXPXG.get())
                return;

            int amp = e.getEffectInstance().getAmplifier(); // 0-based
            setPercent(sp, 1.0 + 0.10 * (amp + 1));
        }

        /** 效果被移除（牛奶/命令/清除） */
        @SubscribeEvent
        public static void onEffectRemove(MobEffectEvent.Remove e) {
            if (!(e.getEntity() instanceof ServerPlayer sp))
                return;
            if (e.getEffect() != ModEffects.FASTEREXPXG.get())
                return;

            setPercent(sp, 1.0);
        }

        /** 效果自然过期 */
        @SubscribeEvent
        public static void onEffectExpired(MobEffectEvent.Expired e) {
            if (!(e.getEntity() instanceof ServerPlayer sp))
                return;
            if (e.getEffectInstance() == null)
                return;
            if (e.getEffectInstance().getEffect() != ModEffects.FASTEREXPXG.get())
                return;

            setPercent(sp, 1.0);
        }

        /** 玩家死亡后重生（Clone）：强制清零 */
        @SubscribeEvent
        public static void onClone(PlayerEvent.Clone e) {
            if (!(e.getEntity() instanceof ServerPlayer sp))
                return;
            PlayerVariables vars = PlayerVariablesProvider.get(sp);
            if (vars != null) {
                vars.exp_global_persent = 1.0;
                // 如有同步方法，可在此同步到客户端（示例：vars.sync(sp);）
            }
        }

        private static void setPercent(ServerPlayer sp, double value) {
            PlayerVariables vars = PlayerVariablesProvider.get(sp);
            if (vars == null)
                return;
            vars.exp_global_persent = value; // 例如 0.0、0.1、0.2 ... 0.5
            // 如需 UI 即时可见，可在此同步（示例：vars.sync(sp);）
        }
    }
}
