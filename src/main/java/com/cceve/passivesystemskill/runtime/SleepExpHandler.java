package com.cceve.passivesystemskill.runtime;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.items.ModItems;
import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SleepExpHandler {

    // 睡觉中的玩家 UUID -> tick 计数
    private static final Map<UUID, Integer> sleeping = new HashMap<>();

    // 每 5 秒一次 (20 tick * 5)
    private static final int INTERVAL = 100;

    // ==================================================
    // 事件：开始睡觉
    // ==================================================
    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp))
            return;
        sleeping.put(sp.getUUID(), 0);
    }

    // ==================================================
    // 事件：玩家醒来
    // ==================================================
    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp))
            return;
        // 发放完整奖励（100%）
        giveReward(sp, 1.0);
        sleeping.remove(sp.getUUID());
    }

    // ==================================================
    // Tick：每 5 秒给一次 5% 效果
    // ==================================================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END)
            return;
        if (!(e.player instanceof ServerPlayer sp))
            return;

        UUID id = sp.getUUID();
        if (!sleeping.containsKey(id))
            return;
        if (!sp.isSleeping()) {
            sleeping.remove(id);
            return;
        }

        int ticks = sleeping.getOrDefault(id, 0) + 1;
        if (ticks >= INTERVAL) {
            ticks = 0;
            // 每 5 秒触发一次 5% 效果
            giveReward(sp, 0.05);
        }
        sleeping.put(id, ticks);
    }

    // ==================================================
    // 发放奖励
    // ==================================================
    private static void giveReward(ServerPlayer sp, double efficiency) {
        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        // 获取 ShiShui 等级
        int sleepLv = getLevel(vars, PlayerVariables.SkillId.ShiShui);

        // 1. 技能经验（随等级增加）
        if (sleepLv > 0) {
            // 每级额外 +0.01 基础经验
            int exp = (int) Math.ceil((5) * efficiency);
            gainSkillExp(sp, PlayerVariables.SkillId.ShiShui, exp);
        }

        // 2. 原版经验（基础 + 精神加成 + ShiShui 等级影响）
        int baseXp = (int) Math.max(1, Math.round((3 + sleepLv * 0.05) * efficiency));
        int spiritLv = getLevel(vars, PlayerVariables.SkillId.JinShen);
        int finalXp = applySpiritBonus(baseXp, spiritLv);
        sp.giveExperiencePoints(finalXp);

        // 3. 掉落 skill_crystal（随等级提高几率）
        // 基础 1% * 效率，每级 ShiShui 再加 0.05%
        double chance = (0.01 + sleepLv * 0.0005) * efficiency;
        if (sp.getRandom().nextDouble() < chance) {
            sp.spawnAtLocation(new ItemStack(ModItems.SKILL_CRYSTAL.get()));
        }
    }

    private static void gainSkillExp(ServerPlayer sp, PlayerVariables.SkillId id, int amount) {
        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;
        PlayerVariables.SkillRecord rec = vars.skillMap.get(id);
        if (rec == null) {
            rec = new PlayerVariables.SkillRecord(0, 0, 5, SkillTreeLibrary.node(id).maxLevel);
            vars.skillMap.put(id, rec);
        }
        int formula = ExpFormula.calcRounded(sp);
        rec.expNow += Math.max(1, amount * formula);

        while (rec.expNow >= rec.expNext && rec.level < SkillTreeLibrary.node(id).maxLevel) {
            rec.expNow -= rec.expNext;
            rec.level++;
            rec.expNext = (int) Math.ceil(rec.expNext * 1.1 + 10);
        }
    }

    private static int getLevel(PlayerVariables vars, PlayerVariables.SkillId id) {
        PlayerVariables.SkillRecord r = vars.skillMap.get(id);
        return r == null ? 0 : r.level;
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
