package com.cceve.passivesystemskill.liandong.tetra;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.liandong.tetra.WorkbenchCraftEvent;
import com.cceve.passivesystemskill.runtime.ExpFormula;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;

@Mod.EventBusSubscriber(modid = "passivesystemskill", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TetraEventHandler {
    private static final int BASE_REQ = 50;

    @SubscribeEvent
    public static void onWorkbenchCraft(WorkbenchCraftEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player))
            return;

        PlayerVariables vars = PlayerVariablesProvider.get(player);
        int lvPlant = levelOf(vars, SkillId.tetra);
        int skilllv = levelOf(vars, SkillId.tetra_quality);
        var def = SkillTreeLibrary.node(PlayerVariables.SkillId.tetra);
        if (def == null)
            return;
        if (lvPlant > 0) {
            if (skilllv > 0) {

                ItemStack upgraded = event.getUpgradedStack();
                if (upgraded.getItem() instanceof se.mickelus.tetra.items.modular.IModularItem modular) {
                    String currentSlot = event.getCurrentSlot();
                    // 遍历所有主模块，找到当前改造的那个
                    for (se.mickelus.tetra.module.ItemModuleMajor module : modular.getMajorModules(upgraded)) {
                        if (module.getSlot().equals(currentSlot)) {
                            // 你要赋予的目标等级
                            int targetQuality = skilllv;
                            module.addImprovement(upgraded, "sl_quality", targetQuality);
                            break;
                        }
                    }
                }
            }
            // player.sendSystemMessage(Component.literal(
            // "§a检测到锻造事件! 槽位: " + event.getCurrentSlot()));

            gainSkillExp(player, PlayerVariables.SkillId.tetra, (int) Math.ceil(BASE_REQ));
            // 2. 原版经验（基础 + 精神加成 + ShiShui 等级影响）
            int baseXp = (int) Math.max(1, Math.round((3 + lvPlant * 0.05)));
            int spiritLv = levelOf(vars, PlayerVariables.SkillId.JinShen);
            int finalXp = applySpiritBonus(baseXp, spiritLv);
            player.giveExperiencePoints(finalXp);
        } else {
            return;
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

    // —— 工具：应用“精神”乘区（100 级 +50% => 每级 +0.5%）——
    private static int applySpiritBonus(int base, int spiritLevel) {
        if (base <= 0 || spiritLevel <= 0)
            return base;

        double mul = 1.0 + 2 * (spiritLevel / 100.0); // 和你之前定义的保持一致
        int out = (int) Math.round(base * mul);
        return Math.max(1, out);
    }
}