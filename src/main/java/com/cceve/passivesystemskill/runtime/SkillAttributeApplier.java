package com.cceve.passivesystemskill.runtime;

import java.util.UUID;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.pss_main;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class SkillAttributeApplier {

    // ===== 固定 UUID：每种“属性修饰符”使用一个，避免叠加/残留 =====
    private static final UUID UUID_LiLiang_ATTACK = UUID.fromString("8b2f1b3f-1f0a-49d0-8a49-9f25a1c3c0a1");
    private static final UUID UUID_TiZi_MAX_HEALTH = UUID.fromString("a0f7fb90-6c7f-41a8-8b1c-9b1c6b2ea001");
    private static final UUID UUID_MinJie_ATTACK_SPEED = UUID.fromString("b1b2c3d4-e5f6-47a8-9101-12ab34cd56ef");
    private static final UUID UUID_ZongJia_ARMOR = UUID.fromString("c2bb6a60-3f5a-4e89-bf6f-7c2a6d4b1234");
    private static final UUID UUID_QinJia_TOUGHNESS = UUID.fromString("d3cc7b71-4a6b-4a9a-8b7f-8d3b7e5c5678");

    // ===== 数值常量（可按需调整） =====
    private static final double LiLiang_ATTACK_PER_LEVEL = 0.05; // 力量 -> 攻击力
    private static final double TiZi_HP_PER_LEVEL = 0.2; // 体质 -> 生命（只取整数）
    private static final double MinJie_AS_PER_LEVEL = 0.02; // 敏捷 -> 攻速
    private static final double WaKuang_BREAK_PER_LEVEL = 0.002; // 挖矿 -> 破坏速度 1+*100*0.02
    private static final double FaMu_BREAK_PER_LEVEL = 0.002; // 伐木 -> 破坏速度 1+*100*0.02
    private static final double DaLi_BREAK_PER_LEVEL = 0.02; // 大力 -> 破坏速度 1*5*0.02
    private static final double ZongJia_ARMOR_PER_LEVEL = 0.05; // 重甲 -> 护甲
    private static final double QinJia_TOUGH_PER_LEVEL = 0.05; // 轻甲 -> 护甲韧性

    // 经验相关：100 级对应 5~20，线性缩放；精神加成为 +50% at 100 级（每级 +0.5%）
    private static final int XP_MAX_LEVEL = 100;
    private static final int XP_RANGE_MIN_AT_MAX = 5;
    private static final int XP_RANGE_MAX_AT_MAX = 20;

    private static final double JINSHEN_BONUS_AT_MAX = 2.00; // +200%其他各处可能也使用了常量，在更改时注意

    private SkillAttributeApplier() {
    }

    /** 尽量不抛异常的包装 */
    public static void applyAllSafe(ServerPlayer sp) {
        try {
            applyAll(sp);
        } catch (Throwable ignored) {
        }
    }

    /** 应用所有“通过属性修饰符即可生效”的加成 */
    public static void applyAll(ServerPlayer sp) {
        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        applyAttackBonus(sp, vars); // 力量 -> 攻击
        applyMaxHealthBonus(sp, vars); // 体质 -> 生命（只取整数）
        applyAttackSpeedBonus(sp, vars); // 敏捷 -> 攻速
        applyArmorBonus(sp, vars); // 重甲 -> 护甲
        applyToughnessBonus(sp, vars); // 轻甲 -> 护甲韧性
        // 破坏速度/钓鱼/种植的经验在事件中处理（见下方订阅器）
    }

    /** 力量：每级 +1 攻击力（保留） */
    private static void applyAttackBonus(ServerPlayer sp, PlayerVariables vars) {
        AttributeInstance inst = sp.getAttribute(Attributes.ATTACK_DAMAGE);
        if (inst == null)
            return;
        int lv = levelOf(vars, SkillId.LiLiang);
        double bonus = lv * LiLiang_ATTACK_PER_LEVEL;
        setAdditive(inst, UUID_LiLiang_ATTACK, "pss_liliang_attack_bonus", bonus);
    }

    /** 体质：每级 +0.2 生命；仅当总加成为整数时应用（即每 5 级 +1） */
    private static void applyMaxHealthBonus(ServerPlayer sp, PlayerVariables vars) {
        AttributeInstance inst = sp.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null)
            return;
        int lv = levelOf(vars, SkillId.TiZi);
        int intBonus = (int) Math.floor(lv * TiZi_HP_PER_LEVEL); // 0,1,2,...
        setAdditive(inst, UUID_TiZi_MAX_HEALTH, "pss_tizi_max_health", intBonus);
        if (sp.getHealth() > sp.getMaxHealth())
            sp.setHealth(sp.getMaxHealth());
    }

    /** 敏捷：每级 +0.02 攻击速度（加法） */
    private static void applyAttackSpeedBonus(ServerPlayer sp, PlayerVariables vars) {
        AttributeInstance inst = sp.getAttribute(Attributes.ATTACK_SPEED);
        if (inst == null)
            return;
        int lv = levelOf(vars, SkillId.MinJie);
        double bonus = lv * MinJie_AS_PER_LEVEL;
        setAdditive(inst, UUID_MinJie_ATTACK_SPEED, "pss_minjie_attack_speed", bonus);
    }

    /** 重甲：每级 +0.05 护甲 */
    private static void applyArmorBonus(ServerPlayer sp, PlayerVariables vars) {
        AttributeInstance inst = sp.getAttribute(Attributes.ARMOR);
        if (inst == null)
            return;
        int lv = levelOf(vars, SkillId.ZongJia);
        double bonus = lv * ZongJia_ARMOR_PER_LEVEL;
        setAdditive(inst, UUID_ZongJia_ARMOR, "pss_zongjia_armor", bonus);
    }

    /** 轻甲：每级 +0.05 护甲韧性 */
    private static void applyToughnessBonus(ServerPlayer sp, PlayerVariables vars) {
        AttributeInstance inst = sp.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (inst == null)
            return;
        int lv = levelOf(vars, SkillId.QinJia);
        double bonus = lv * QinJia_TOUGH_PER_LEVEL;
        setAdditive(inst, UUID_QinJia_TOUGHNESS, "pss_qinjia_toughness", bonus);
    }

    // ===== 工具函数 =====

    /** 空安全读等级 */
    private static int levelOf(PlayerVariables vars, SkillId id) {
        if (vars == null || vars.skillMap == null || id == null)
            return 0;
        var rec = vars.skillMap.get(id);
        return rec != null ? rec.level : 0;
    }

    /** 设置“加法”修饰符（先移除旧的再添加） */
    private static void setAdditive(AttributeInstance inst, UUID uuid, String name, double amount) {
        var old = inst.getModifier(uuid);
        if (old != null)
            inst.removeModifier(uuid);
        if (amount != 0.0) {
            inst.addPermanentModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    /*
     * ======================================================================
     * Forge 事件订阅：
     * - 挖矿/伐木：方块破坏速度提升
     * - 钓鱼：上钩时发经验
     * - 种植：破坏“成熟作物”时发经验
     * 经验全部受“精神(JinShen)”乘区加成
     * ======================================================================
     */
    @Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

        /** 破坏速度：挖矿/伐木加成（客户端/服务端都能改“显示速度”即可） */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
            if (e.getEntity() == null || e.getState() == null)
                return;

            PlayerVariables vars = e.getEntity().getCapability(PlayerVariablesProvider.CAPABILITY).orElse(null);
            if (vars == null)
                return;

            int lvMine = levelOf(vars, SkillId.WaKuang);
            int lvAxe = levelOf(vars, SkillId.FaMu);
            int lvdl = levelOf(vars, SkillId.Skill_DaLi);

            float speed = e.getNewSpeed();
            float bonus = 0f;
            if (lvMine > 0 && e.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                bonus += ((float) (lvMine * WaKuang_BREAK_PER_LEVEL)); // 0.1%/级
            }
            if (lvAxe > 0 && e.getState().is(BlockTags.MINEABLE_WITH_AXE)) {
                bonus += ((float) (lvAxe * FaMu_BREAK_PER_LEVEL)); // 0.1%/级
            }
            if (lvdl > 0) {
                bonus += ((float) (lvdl * DaLi_BREAK_PER_LEVEL)); // 1%/级
            }
            speed *= (1.0f + bonus);
            e.setNewSpeed(speed);
        }

        /** 钓鱼：上钩时给经验（只在服务端执行） */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onItemFished(ItemFishedEvent e) {
            if (!(e.getEntity() instanceof ServerPlayer sp))
                return;

            PlayerVariables vars = PlayerVariablesProvider.get(sp);
            if (vars == null)
                return;

            int lvFish = levelOf(vars, SkillId.DiaoYu);
            if (lvFish <= 0)
                return;

            int base = randomXpForLevel(sp, lvFish);
            int spiritLv = levelOf(vars, SkillId.JinShen);
            int finalXp = applySpiritBonus(base, spiritLv);

            if (finalXp > 0)
                sp.giveExperiencePoints(finalXp);
        }

        /** 种植：破坏“成熟作物”时给经验（只在服务端执行） */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onBreakBlockExp(net.minecraftforge.event.level.BlockEvent.BreakEvent e) {
            if (!(e.getPlayer() instanceof ServerPlayer sp))
                return;

            BlockState state = e.getState();
            if (state == null)
                return;

            // 仅处理 CropBlock（小麦/胡萝卜/马铃薯/甜菜根等）
            if (!(state.getBlock() instanceof CropBlock crop))
                return;
            if (!crop.isMaxAge(state))
                return; // 仅成熟

            PlayerVariables vars = PlayerVariablesProvider.get(sp);
            if (vars == null)
                return;

            int lvPlant = levelOf(vars, SkillId.Zhongzhi);
            if (lvPlant <= 0)
                return;

            int base = randomXpForLevel(sp, lvPlant);
            int spiritLv = levelOf(vars, SkillId.JinShen);
            int finalXp = applySpiritBonus(base, spiritLv);

            if (finalXp > 0)
                sp.giveExperiencePoints(finalXp);
        }

        @SubscribeEvent
        public static void onPlayerUnlucky(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
            if (!(e.getEntity() instanceof ServerPlayer sp))
                return;
            if (sp.isCreative() || sp.isSpectator())
                return;

            PlayerVariables vars = PlayerVariablesProvider.get(sp);
            if (vars == null)
                return;

            var src = e.getSource();
            float dmg = e.getAmount();
            int lv = levelOf(vars, SkillId.DaoMei);
            if (lv <= 0)
                return;
            // 触发条件：爆炸 / 雷劈 / 窒息
            boolean unlucky = false;
            if (src.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION))
                unlucky = true;
            if (src.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT))
                unlucky = true;
            if (src.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL))
                unlucky = true;

            if (!unlucky || dmg <= 0)
                return;

            // ===== 给予原版经验点 =====
            int base = Math.max(1, (int) Math.ceil(dmg) / 5); // 按伤害值换经验
            int spiritLv = levelOf(vars, SkillId.JinShen);
            int finalXp = applySpiritBonus(base, spiritLv);
            sp.giveExperiencePoints(finalXp);

            // ===== 极低概率掉落 skill_crystal（物品形式） =====
            double chance = 0.01 + (lv) * 0.001;
            if (sp.getRandom().nextDouble() < chance) {
                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                        new net.minecraft.resources.ResourceLocation(pss_main.MODID, "skill_crystal"));
                if (item != null) {
                    var drop = new net.minecraft.world.item.ItemStack(item);
                    var entity = new net.minecraft.world.entity.item.ItemEntity(
                            sp.level(),
                            sp.getX(), sp.getY() + 0.5, sp.getZ(),
                            drop);
                    // 设置一个小的拾取延迟（可选）
                    entity.setPickUpDelay(5);
                    sp.level().addFreshEntity(entity);
                }
            }
        }

        // —— 工具：按等级生成 [min..max] 区间的经验，并保证 >0 级时至少 1 点 ——
        private static int randomXpForLevel(ServerPlayer sp, int level) {
            double ratio = Math.min(1.0, Math.max(0.0, level / (double) XP_MAX_LEVEL));
            int min = (int) Math.floor(XP_RANGE_MIN_AT_MAX * ratio);
            int max = (int) Math.ceil(XP_RANGE_MAX_AT_MAX * ratio);
            if (level > 0)
                min = Math.max(1, min);
            if (max < min)
                max = min;
            // 包含端点
            return min + sp.getRandom().nextInt(max - min + 1);
        }

        // —— 工具：应用“精神”乘区（100 级 +50% => 每级 +0.5%）——
        private static int applySpiritBonus(int base, int spiritLevel) {
            if (base <= 0 || spiritLevel <= 0)
                return base;
            double mul = 1.0 + JINSHEN_BONUS_AT_MAX * (spiritLevel / (double) XP_MAX_LEVEL);
            int out = (int) Math.round(base * mul);
            return Math.max(1, out);
        }
    }
}
