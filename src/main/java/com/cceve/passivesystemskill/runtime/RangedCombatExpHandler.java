package com.cceve.passivesystemskill.runtime;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RangedCombatExpHandler {

    private static final int BASE_REQ = 5;
    private static final float BONUS_PER_LEVEL = 0.01f;

    @SubscribeEvent
    public static void onRangedAttack(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (sp.isCreative() || sp.isSpectator())
            return;

        Entity direct = e.getSource().getDirectEntity();
        if (!(direct instanceof Projectile)) {
            return; // 不是远程/投掷物
        }
        var test = SkillTreeLibrary.node(PlayerVariables.SkillId.SheJi);
        if (test == null) {
            // 没定义这个技能，直接跳过，避免NPE
            return;
        }

        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        LivingEntity target = e.getEntity();
        if (target == null)
            return;

        double distance = sp.distanceTo(target);
        double damage = e.getAmount();

        // 远程熟练度（伤害加成）
        gainSkillExp(sp, PlayerVariables.SkillId.SheJi, (int) Math.ceil(distance));

        int lv = getLevel(vars, PlayerVariables.SkillId.SheJi);
        if (lv > 0) {
            float bonus = (float) (damage * lv * BONUS_PER_LEVEL);
            DamageSource src = sp.damageSources().playerAttack(sp);
            // 移除无敌帧
            target.invulnerableTime = 0; // 1.20.1+
            target.hurt(src, bonus);
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

        // sp.sendSystemMessage(Component.literal("射击：经验 " + amount + " 点"));
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

    private static int getLevel(PlayerVariables vars, PlayerVariables.SkillId id) {
        PlayerVariables.SkillRecord r = vars.skillMap.get(id);
        return r == null ? 0 : r.level;
    }
}
