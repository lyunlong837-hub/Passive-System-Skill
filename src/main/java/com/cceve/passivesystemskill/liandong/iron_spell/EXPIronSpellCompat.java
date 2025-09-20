package com.cceve.passivesystemskill.liandong.iron_spell;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.runtime.ExpFormula;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;
import com.cceve.passivesystemskill.registry.ModEffects;

// 注意：这里不要直接 import SpellOnCastEvent！
// 我们用反射/安全调用方式来避免类缺失导致崩溃

@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EXPIronSpellCompat {

    @SubscribeEvent
    public static void onAnyEvent(net.minecraftforge.eventbus.api.Event event) {
        // 检查 Iron's Spellbooks 是否加载
        if (!ModList.get().isLoaded("irons_spellbooks"))
            return;

        // 检查是不是 SpellOnCastEvent（用类名判断，避免直接引用）
        if (event.getClass().getName().equals("io.redspace.ironsspellbooks.api.events.SpellOnCastEvent")) {
            handleSpellCast(event);
        }
    }

    // 用反射安全获取数据
    private static void handleSpellCast(Object event) {
        try {
            // 反射拿 entity
            Object entity = event.getClass().getMethod("getEntity").invoke(event);
            if (!(entity instanceof ServerPlayer player))
                return;

            int manaCost = (int) event.getClass().getMethod("getManaCost").invoke(event);

            PlayerVariables vars = PlayerVariablesProvider.get(player);
            if (vars == null)
                return;

            // ====== 铁魔法经验获取 ======
            int lv = getLevel(vars, PlayerVariables.SkillId.IronSpell);
            if (lv < 1)
                return;

            int baseExp = ExpFormula.calcRounded(player);
            int gain = (int) Math.round(manaCost * baseExp * 0.1);
            if (gain > 0) {
                gainSkillExp(player, PlayerVariables.SkillId.IronSpell, gain);
            }

            // ====== 法强 Buff 概率触发 ======
            int lv2 = getLevel(vars, PlayerVariables.SkillId.IronSpellFaQiang);
            if (lv2 >= 1) {
                double chance = lv2 * 0.01;
                if (player.getRandom().nextDouble() < chance) {
                    player.addEffect(new MobEffectInstance(
                            ModEffects.MAGIC_POWER.get(),
                            20 * lv2, // tick
                            0,
                            false,
                            true));
                }
            }
        } catch (Exception e) {
            // 捕获反射错误，避免崩溃
            e.printStackTrace();
        }
    }

    private static int getLevel(PlayerVariables vars, PlayerVariables.SkillId id) {
        PlayerVariables.SkillRecord r = vars.skillMap.get(id);
        return r == null ? 0 : r.level;
    }

    private static void gainSkillExp(ServerPlayer player, PlayerVariables.SkillId id, int gain) {
        SkillTreeLibrary.NodeDef def = SkillTreeLibrary.node(id);
        if (def == null)
            return;

        player.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(v -> {
            PlayerVariables.SkillRecord rec = v.skillMap.get(id);
            if (rec != null) {
                rec.expNow += gain;
                while (rec.expNow >= rec.expNext && rec.level < def.maxLevel) {
                    rec.expNow -= rec.expNext;
                    rec.level++;
                    rec.expNext = (int) Math.ceil(rec.expNext * 1.1 + 5);
                }
            }
        });
    }
}
