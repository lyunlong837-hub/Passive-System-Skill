package com.cceve.passivesystemskill.runtime;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家受到攻击时获得技能经验：
 * - 护甲值 >=15：重甲经验 = 护甲值
 * - 护甲值 <=10：轻甲经验 = 护甲值
 * - 体质经验 = 伤害值
 */
@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DefenseExpHandler {

    private DefenseExpHandler() {
    }

    private static final int BASE_REQ = 5;

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp))
            return;
        if (sp.isCreative() || sp.isSpectator())
            return;

        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        int armor = sp.getArmorValue();
        float damage = e.getAmount();

        var node = SkillTreeLibrary.node(PlayerVariables.SkillId.ZongJia);
        if (node == null) {
            return;
        }

        // 重甲
        if (armor >= 15) {
            int lv = getLevel(vars, PlayerVariables.SkillId.ZongJia);
            if (lv >= 1) {
                gainSkillExp(sp, PlayerVariables.SkillId.ZongJia, armor);
            }
        }

        // 轻甲
        if (armor <= 10 && armor >= 1) {
            int lv = getLevel(vars, PlayerVariables.SkillId.QinJia);
            if (lv >= 1) {
                gainSkillExp(sp, PlayerVariables.SkillId.QinJia, (11 - armor) * 5);
            }
        }

        // 体质（始终加）
        if (damage > 0) {
            int lv = getLevel(vars, PlayerVariables.SkillId.TiZi);
            if (lv >= 1) {
                gainSkillExp(sp, PlayerVariables.SkillId.TiZi, (int) Math.ceil(damage));
            }
        }

        // 倒霉：爆炸、雷劈、窒息
        DamageSource src = e.getSource();
        float dmg = e.getAmount(); // 本次伤害量
        boolean unlucky = false;

        // 爆炸伤害（包括 TNT/苦力怕/火球）
        if (src.is(DamageTypes.EXPLOSION)) {
            unlucky = true;
        }

        // 雷劈伤害
        if (src.is(DamageTypes.LIGHTNING_BOLT)) {
            unlucky = true;
        }

        // 窒息伤害（方块卡头）
        if (src.is(DamageTypes.IN_WALL)) {
            unlucky = true;
        }

        if (unlucky) {
            int lv = getLevel(vars, PlayerVariables.SkillId.DaoMei);
            if (lv >= 1) {
                // 这里经验量你可以自己定义，比如固定+5
                int exp = Math.max(1, (int) Math.ceil(dmg) * 5); // 根据伤害值加经验，至少1点
                gainSkillExp(sp, PlayerVariables.SkillId.DaoMei, exp);
            }
        }

    }

    /** 给指定技能加经验 */
    private static void gainSkillExp(ServerPlayer sp, PlayerVariables.SkillId id, int amount) {
        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        PlayerVariables.SkillRecord rec = vars.skillMap.get(id);
        if (rec == null) {
            rec = new PlayerVariables.SkillRecord(0, 0, BASE_REQ, SkillTreeLibrary.node(id).maxLevel);
            vars.skillMap.put(id, rec);
        }
        if (rec.expNext <= 0)
            rec.expNext = BASE_REQ;

        int formula = ExpFormula.calcRounded(sp);
        rec.expNow += Math.max(1, amount * formula);

        while (rec.expNow >= rec.expNext && rec.level < SkillTreeLibrary.node(id).maxLevel) {
            rec.expNow -= rec.expNext;
            rec.level++;
            rec.expNext = nextFrom(rec.expNext);
        }
    }

    /** 升级需求公式 */
    private static int nextFrom(int prevReq) {
        double prev = Math.max(1.0, prevReq);
        return (int) Math.ceil(prev * 1.1 + 10);
    }

    private static int getLevel(PlayerVariables vars, PlayerVariables.SkillId id) {
        PlayerVariables.SkillRecord r = vars.skillMap.get(id);
        return r == null ? 0 : r.level;
    }
}
