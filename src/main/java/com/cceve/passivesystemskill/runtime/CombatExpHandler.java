package com.cceve.passivesystemskill.runtime;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatExpHandler {

    private static final int BASE_REQ = 5;

    @SubscribeEvent
    public static void onAttack(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (sp.isCreative() || sp.isSpectator())
            return;

        var node = SkillTreeLibrary.node(PlayerVariables.SkillId.LiLiang);
        if (node == null) {
            return;
        }

        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        LivingEntity target = e.getEntity();
        if (target == null)
            return;

        double playerAttack = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                .getValue();
        double damage = e.getAmount();
        // 判断是否完全蓄力

        float cooldown = sp.getAttackStrengthScale(0.5F);
        if (cooldown >= 0.99F) {
            // 力量：根据自身攻击力获得经验
            gainSkillExp(sp, PlayerVariables.SkillId.LiLiang, (int) Math.ceil(playerAttack));
        } else {
            // 敏捷：根据伤害获得经验
            gainSkillExp(sp, PlayerVariables.SkillId.MinJie, (int) Math.ceil(damage));
        }
        // === 1) 骑乘时获得经验 ===
        if (sp.getVehicle() != null) {
            gainSkillExp(sp, PlayerVariables.SkillId.QiShu, (int) Math.ceil(damage));

        }
        // 驱邪：攻击亡灵生物
        if (target.getMobType() == MobType.UNDEAD) {
            gainSkillExp(sp, PlayerVariables.SkillId.QuXie, (int) Math.ceil(damage));
        }

        // 断肢：攻击节肢生物
        if (target.getMobType() == MobType.ARTHROPOD) {
            gainSkillExp(sp, PlayerVariables.SkillId.DuanZhi, (int) Math.ceil(damage));
        }

        // 荣誉：攻击 攻击力*2大于自身血量的生物
        double targetAttack = target
                .getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null
                        ? target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                                .getValue()
                        : 0;
        if (targetAttack * 2 > sp.getMaxHealth()) {
            gainSkillExp(sp, PlayerVariables.SkillId.RongYu, (int) Math.ceil(damage));
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
}
