package com.cceve.passivesystemskill.runtime;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.pss_main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExtraDamageHandler {

    private static final float BONUS_PER_LEVEL = 0.001f;

    // 记录玩家上次触发的时间（tick）
    private static final Map<ServerPlayer, Integer> lastTriggerTick = new WeakHashMap<>();

    @SubscribeEvent
    public static void onAttack(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer sp))
            return;
        if (sp.isCreative() || sp.isSpectator())
            return;

        LivingEntity target = e.getEntity();
        if (target == null)
            return;

        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        int gameTime = sp.level().getGameTime() > Integer.MAX_VALUE
                ? (int) (sp.level().getGameTime() % Integer.MAX_VALUE)
                : (int) sp.level().getGameTime();

        // 检查冷却：小于 2tick 不触发
        if (lastTriggerTick.containsKey(sp) && gameTime - lastTriggerTick.get(sp) < 2) {
            return;
        }
        lastTriggerTick.put(sp, gameTime);
        double playerAttack = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                .getValue();
        // ========== 驱邪：攻击亡灵 ==========
        if (target.getMobType() == MobType.UNDEAD) {
            int lv = getLevel(vars, PlayerVariables.SkillId.QuXie);
            if (lv > 0) {
                float bonus = (float) (playerAttack * lv * BONUS_PER_LEVEL);
                DamageSource src = sp.damageSources().playerAttack(sp);
                // 移除无敌帧
                target.invulnerableTime = 0; // 1.20.1+
                target.hurt(src, bonus);
                // sp.sendSystemMessage(Component.literal("驱邪：额外 " + bonus + " 伤害"));
            }
        }

        // ========== 断肢：攻击节肢 ==========
        if (target.getMobType() == MobType.ARTHROPOD) {
            int lv = getLevel(vars, PlayerVariables.SkillId.DuanZhi);
            if (lv > 0) {
                float bonus = (float) (playerAttack * lv * BONUS_PER_LEVEL);
                DamageSource src = sp.damageSources().playerAttack(sp);
                target.invulnerableTime = 0; // 1.20.1+
                target.hurt(src, bonus);
                // sp.sendSystemMessage(Component.literal("断肢：额外 " + bonus + " 伤害"));
            }
        }

        // ========== 骑乘攻击 ==========
        if (sp.getVehicle() != null) {
            int lv = getLevel(vars, PlayerVariables.SkillId.QiShu);
            if (lv > 0) {
                float bonus = (float) (playerAttack * lv * BONUS_PER_LEVEL);
                DamageSource src = sp.damageSources().playerAttack(sp);
                target.invulnerableTime = 0; // 1.20.1+
                target.hurt(src, bonus);
                // sp.sendSystemMessage(Component.literal("断肢：额外 " + bonus + " 伤害"));
            }
        }

        // ========== 荣誉：攻击强敌 ==========
        float targetAttack = target
                .getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null
                        ? (float) target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                                .getValue()
                        : 0;
        float playerHealth = sp.getMaxHealth();
        if (targetAttack * 2 > playerHealth) {
            int lv = getLevel(vars, PlayerVariables.SkillId.RongYu);
            if (lv > 0) {
                float bonus = (float) (playerAttack * lv * BONUS_PER_LEVEL);
                DamageSource src = sp.damageSources().playerAttack(sp);
                target.invulnerableTime = 0; // 1.20.1+
                target.hurt(src, bonus);
                // sp.sendSystemMessage(Component.literal("荣誉：额外 " + bonus + " 伤害"));
            }
        }

    }

    private static int getLevel(PlayerVariables vars, PlayerVariables.SkillId id) {
        PlayerVariables.SkillRecord rec = vars.skillMap.get(id);
        return rec != null ? rec.level : 0;
    }
}
