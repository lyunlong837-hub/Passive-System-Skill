package com.cceve.passivesystemskill.brewing;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.registry.ModPotions;
import com.cceve.passivesystemskill.items.ModItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** 在原版酿造台里添加自定义配方 */
@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBrewing {

    private ModBrewing() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            // 1) 粗制药水 + 熟练度水晶 -> 熟练专精药水
            BrewingRecipeRegistry.addRecipe(new SimplePotionMix(
                    Potions.AWKWARD,
                    new ItemStack(ModItems.SKILL_CRYSTAL.get()), // ← 替换成你的“熟练度水晶”物品
                    ModPotions.FASTEREXPXG.get()));

            // 2) 熟练专精药水 + 红石 -> 长效熟练专精
            BrewingRecipeRegistry.addRecipe(new SimplePotionMix(
                    ModPotions.FASTEREXPXG.get(),
                    new ItemStack(Items.REDSTONE),
                    ModPotions.FASTEREXPXG_LONG.get()));

            // 3) 熟练专精药水 + 萤石粉 -> 强效熟练专精
            BrewingRecipeRegistry.addRecipe(new SimplePotionMix(
                    ModPotions.FASTEREXPXG.get(),
                    new ItemStack(Items.GLOWSTONE_DUST),
                    ModPotions.FASTEREXPXG_STRONG.get()));
        });
    }

    /** 一个简单的酿造配方实现：指定 输入药水 / 原料物品 / 输出药水 */
    private static class SimplePotionMix implements IBrewingRecipe {
        private final net.minecraft.world.item.alchemy.Potion inputPotion;
        private final ItemStack ingredient;
        private final net.minecraft.world.item.alchemy.Potion outputPotion;

        private SimplePotionMix(net.minecraft.world.item.alchemy.Potion inputPotion,
                ItemStack ingredient,
                net.minecraft.world.item.alchemy.Potion outputPotion) {
            this.inputPotion = inputPotion;
            this.ingredient = ingredient;
            this.outputPotion = outputPotion;
        }

        @Override
        public boolean isInput(ItemStack stack) {
            // 必须是“药水”物品，且内部的 Potion 匹配
            return stack.getItem() == Items.POTION
                    && PotionUtils.getPotion(stack) == inputPotion;
        }

        @Override
        public boolean isIngredient(ItemStack stack) {
            return ItemStack.isSameItemSameTags(stack, ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredientStack) {
            if (!isInput(input) || !isIngredient(ingredientStack))
                return ItemStack.EMPTY;
            // 产出一瓶新药水（不复制输入瓶的 NBT）
            return PotionUtils.setPotion(new ItemStack(Items.POTION), outputPotion);
        }
    }
}
