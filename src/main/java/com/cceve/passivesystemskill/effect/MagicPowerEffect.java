package com.cceve.passivesystemskill.effect;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.liandong.iron_spell.ISSAttribute;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class MagicPowerEffect extends MobEffect {
    public MagicPowerEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7f00ff); // 紫色效果图标
    }

    /**
     * 每 tick 调用（持续刷新数值，防止丢失）
     */
    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap map,
            int amplifier) {
        super.addAttributeModifiers(entity, map, amplifier);

        if (entity instanceof ServerPlayer sp) {
            PlayerVariables vars = PlayerVariablesProvider.get(sp);
            if (vars != null) {
                int bonus = (amplifier + 1);
                ISSAttribute.magicPowerBonus(sp, vars, bonus);
            }
        }
    }

    /**
     * 效果是否每 tick 触发
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // 每 tick 执行 applyEffectTick
    }

    /**
     * 药水移除时调用，清除加成
     */
    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap map,
            int amplifier) {
        super.removeAttributeModifiers(entity, map, amplifier);

        if (entity instanceof ServerPlayer sp) {
            PlayerVariables vars = PlayerVariablesProvider.get(sp);
            if (vars != null) {
                // 移除加成
                ISSAttribute.magicPowerBonus(sp, vars, 0);
                // vars.sync(sp);
            }
        }
    }
}