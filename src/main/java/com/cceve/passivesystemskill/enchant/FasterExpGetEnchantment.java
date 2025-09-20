package com.cceve.passivesystemskill.enchant;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/** 熟练专精（Fasterexpget） */
public class FasterExpGetEnchantment extends Enchantment {

    /** 自定义分类：允许所有物品 */
    public static final EnchantmentCategory ALL_ITEMS = EnchantmentCategory.create("pss_all_items", item -> true);

    public FasterExpGetEnchantment() {
        // 稀有度 RARE；分类 ALL_ITEMS；作用到所有装备槽位（不限制）
        super(Rarity.RARE, ALL_ITEMS, EquipmentSlot.values());
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean isTreasureOnly() {
        return false;
    } // 不是宝藏

    @Override
    public boolean isCurse() {
        return false;
    } // 不是诅咒

    @Override
    public boolean isTradeable() {
        return true;
    } // 可用于村民交易

    @Override
    public boolean isDiscoverable() {
        return true;
    } // 可在附魔台/战利品中被发现（非宝藏即可）

    @Override
    public boolean isAllowedOnBooks() {
        return true;
    } // 可以出现在书上

    /** 允许用附魔书在铁砧“套到任何物品” */
    @Override
    public boolean canEnchant(ItemStack stack) {
        return true;
    }

    /** 允许在附魔台对任何可附魔的物品出现 */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return true;
    }
}
