package com.cceve.passivesystemskill.runtime;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.config.Config;
import com.cceve.passivesystemskill.registry.ModEnchantments;
import com.cceve.passivesystemskill.registry.ModEffects;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;

/**
 * 经验公式：
 * 随机(1~3) × (1 + 0.5×熟练总等级) × exp_global_persent × (1 + 1.0×药水等级) × (1 +
 * 智力/100) → 四舍五入
 */
public final class ExpFormula {

    private ExpFormula() {
    }

    /** 只在“真正发放技能经验”的那一刻调用 */
    public static int calcRounded(Player p) {
        // 1) 随机小数 ∈ [1, 3]
        double base = Mth.nextDouble(p.getRandom(), 1.0, 3.0);
        // 2) 装备专精附魔等级
        int totalLvl = 0;
        for (ItemStack s : p.getArmorSlots())
            totalLvl += levelOn(s);
        totalLvl += levelOn(p.getMainHandItem());
        totalLvl += levelOn(p.getOffhandItem());

        double enchMul = 1.0 + 0.5 * totalLvl;

        // 3) 全局倍率
        double globalMul = Config.SERVER.exp_global_config.get();
        double intelligenceMul = 1.0;
        PlayerVariables vars = p.getCapability(PlayerVariablesProvider.CAPABILITY).orElse(null);
        int lv = levelOf(vars, SkillId.ZhiLi);
        int skill_lv = levelOf(vars, SkillId.Skill_dashi);
        if (vars != null) {
            // 智力加成：每点 +1%
            if (lv > 0) {
                intelligenceMul = 1.0 + lv / 100.0;
            }
            // 大师加成：基础值+2每级
            if (skill_lv > 0) {
                base += (skill_lv * 2);
            }
        }

        // 4) 药水加成
        double effectMul = 1.0;
        MobEffectInstance eff = p.getEffect(ModEffects.FASTEREXPXG.get());
        if (eff != null) {
            int amplifier = eff.getAmplifier(); // Lv0=1级, Lv1=2级
            effectMul = 1.0 + (amplifier + 1) * 1.0;
        }

        // 5) 最终结果
        double val = base * enchMul * globalMul * effectMul * intelligenceMul;
        // p.sendSystemMessage(Component.literal("值 " + val));
        return (int) Math.round(val);
    }

    private static int levelOn(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return 0;
        return stack.getEnchantmentLevel(ModEnchantments.FASTEREXPGET.get());
    }

    /** 空安全读等级 */
    private static int levelOf(PlayerVariables vars, SkillId id) {
        if (vars == null || vars.skillMap == null || id == null)
            return 0;
        var rec = vars.skillMap.get(id);
        return rec != null ? rec.level : 0;
    }

}
