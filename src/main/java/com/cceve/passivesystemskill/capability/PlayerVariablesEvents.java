package com.cceve.passivesystemskill.capability;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.liandong.iron_spell.ISSAttribute;
import com.cceve.passivesystemskill.network.ModNetwork;
import com.cceve.passivesystemskill.runtime.SkillAttributeApplier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerVariablesEvents {

    /** 1) 给玩家挂载能力 */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof Player) {
            e.addCapability(PlayerVariablesProvider.KEY, new PlayerVariablesProvider());
        }
    }

    /** 2) 克隆时复制能力（死亡与跨维度都会触发 Clone） */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        // 旧玩家的 capability 在此时已失效，需要临时复活读取
        e.getOriginal().reviveCaps();

        var oldOpt = e.getOriginal().getCapability(PlayerVariablesProvider.CAPABILITY);
        var newOpt = e.getEntity().getCapability(PlayerVariablesProvider.CAPABILITY);

        oldOpt.ifPresent(oldVars -> newOpt.ifPresent(newVars -> newVars.copyFrom(oldVars)));

        // 用完把旧能力再次失效，遵守 Forge 规范
        e.getOriginal().invalidateCaps();

        // 3) 复制完后，下一 tick 在服务端重算属性并同步整块数据到客户端
        if (e.getEntity() instanceof ServerPlayer sp) {
            sp.server.execute(() -> {
                newOpt.ifPresent(vars -> {
                    SkillAttributeApplier.applyAllSafe(sp); // 不会 NPE 的安全版本
                    ISSAttribute.applyAllSafe(sp);
                    ModNetwork.syncToClient(sp, vars);
                });
            });
        }
    }

    /** （可选）登录/跨维度时再同步一次，确保客户端 UI 立即拿到一致数据 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            sp.server.execute(() -> {
                sp.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(vars -> {
                    SkillAttributeApplier.applyAllSafe(sp);
                    ISSAttribute.applyAllSafe(sp);
                    ModNetwork.syncToClient(sp, vars);
                });
            });
        }
    }

    @SubscribeEvent
    public static void onChangedDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            sp.server.execute(() -> {
                sp.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(vars -> {
                    SkillAttributeApplier.applyAllSafe(sp);
                    ISSAttribute.applyAllSafe(sp);
                    ModNetwork.syncToClient(sp, vars);
                });
            });
        }
    }
}
