package com.cceve.passivesystemskill.runtime;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SkillFuMoyxzs {

    @SubscribeEvent
    public static void onEnchantmentLevelSet(EnchantmentLevelSetEvent e) {
        // 找到附近 9 格的玩家
        var players = e.getLevel().getEntitiesOfClass(ServerPlayer.class, new AABB(e.getPos()).inflate(9));
        for (var player : players) {
            PlayerVariables vars = PlayerVariablesProvider.get(player);
            int lvPlant = levelOf(vars, SkillId.XueXi);
            if (lvPlant <= 0)
                return;
            // 确保这个玩家当前打开的容器就是附魔台
            if (player.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu) {

                int skillLevel = lvPlant / 10;

                if (skillLevel >= 1 && e.getEnchantLevel() > 0) {
                    int oldLevel = e.getEnchantLevel();
                    int newLevel = Math.max(1, oldLevel - skillLevel);
                    e.setEnchantLevel(newLevel);
                }

            }
        }
    }

    /** 空安全读等级 */
    private static int levelOf(PlayerVariables vars, SkillId id) {
        if (vars == null || vars.skillMap == null || id == null)
            return 0;
        var rec = vars.skillMap.get(id);
        return rec != null ? rec.level : 0;
    }
}
