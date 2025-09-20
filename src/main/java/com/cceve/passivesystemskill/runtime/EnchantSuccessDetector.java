package com.cceve.passivesystemskill.runtime;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerXpEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;

import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber
public class EnchantSuccessDetector {

    private static final int BASE_REQ = 20;

    @SubscribeEvent
    public static void onPlayerXPChange(PlayerXpEvent.LevelChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        // 只处理经验减少的情况
        if (event.getLevels() >= 0)
            return;
        PlayerVariables vars = PlayerVariablesProvider.get(player);
        int lvPlant = levelOf(vars, SkillId.XueXi);
        if (lvPlant <= 0)
            return;
        var def = SkillTreeLibrary.node(PlayerVariables.SkillId.XueXi);
        if (def == null)
            return;

        // 检查当前打开的界面是不是附魔台
        if (player.containerMenu instanceof EnchantmentMenu menu) {
            // 给玩家发送提示信息
            // player.sendSystemMessage(Component.literal("完成了一次附魔!"));
            gainSkillExp(player, PlayerVariables.SkillId.XueXi, (int) Math.ceil(BASE_REQ));
        }
    }

    private static void gainSkillExp(ServerPlayer sp, PlayerVariables.SkillId id, int amount) {
        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        PlayerVariables.SkillRecord rec = vars.skillMap.get(id);
        if (rec == null) {
            var def = SkillTreeLibrary.node(id);
            if (def == null)
                return;
            rec = new PlayerVariables.SkillRecord(0, 0, BASE_REQ, def.maxLevel);
            vars.skillMap.put(id, rec);
        }
        if (rec.expNext <= 0)
            rec.expNext = BASE_REQ;

        int formula = ExpFormula.calcRounded(sp);
        rec.expNow += Math.max(1, amount * formula);
        // sp.sendSystemMessage(Component.literal("获得了 " + amount * formula + " 点经验！"));
        while (rec.expNow >= rec.expNext && rec.level < SkillTreeLibrary.node(id).maxLevel) {
            rec.expNow -= rec.expNext;
            rec.level++;
            rec.expNext = nextFrom(rec.expNext);
        }
    }

    private static int nextFrom(int prevReq) {
        double prev = Math.max(1.0, prevReq);
        return (int) Math.ceil(prev * 1.1 + 10);
    }

    private static int levelOf(PlayerVariables vars, SkillId id) {
        if (vars == null || vars.skillMap == null || id == null)
            return 0;
        var rec = vars.skillMap.get(id);
        return rec != null ? rec.level : 0;
    }
}
