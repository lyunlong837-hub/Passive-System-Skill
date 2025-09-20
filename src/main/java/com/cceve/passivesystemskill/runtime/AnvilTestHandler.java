package com.cceve.passivesystemskill.runtime;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;

@Mod.EventBusSubscriber
public class AnvilTestHandler {
    private static final int BASE_REQ = 20;

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player))
            return;

        PlayerVariables vars = PlayerVariablesProvider.get(player);
        int lvPlant = levelOf(vars, SkillId.DuanZao);
        var def = SkillTreeLibrary.node(PlayerVariables.SkillId.DuanZao);
        if (def == null)
            return;

        if (lvPlant > 0) {
            int newCost = Math.max(1, event.getCost() - (lvPlant / 10));
            int or = event.getCost();
            ItemStack left = event.getLeft();
            ItemStack right = event.getRight();
            if (left.isEmpty())
                return;
            // 默认结果 = 左边物品的 copy
            ItemStack output = event.getOutput().isEmpty() ? left.copy() : event.getOutput();
            if (!left.isEmpty() && right.isEmpty() && (event.getName() == null || event.getName().isEmpty())) {
                event.setOutput(ItemStack.EMPTY); // 不生成右侧物品
                event.setCost(0); // 经验消耗清零
                return; // 提前结束
            }

            event.setOutput(output);
            event.setCost(newCost);
            // player.sendSystemMessage(Component.literal(
            // "§a需求等级减少到: " + newCost + " 原花费 " + or + " 节点等级 " + lvPlant));

        }
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var node = SkillTreeLibrary.node(PlayerVariables.SkillId.DuanZao);
            if (node == null) {
                return;
            }

            PlayerVariables vars = PlayerVariablesProvider.get(player);
            int lvPlant = levelOf(vars, SkillId.DuanZao);
            if (lvPlant <= 0)
                return;

            // 发放技能经验
            gainSkillExp(player, PlayerVariables.SkillId.DuanZao, BASE_REQ);
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

        int formula = ExpFormula.calcRounded(sp); // 你已有的类
        rec.expNow += Math.max(1, amount * formula);

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
