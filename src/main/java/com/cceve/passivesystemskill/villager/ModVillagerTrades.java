package com.cceve.passivesystemskill.villager;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.items.ModItems;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModVillagerTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        // 给所有图书管理员增加一个交易（你也可以换成其他职业）
        if (event.getType() == VillagerProfession.LIBRARIAN) {
            event.getTrades().get(1).add((entity, random) -> {
                // 第1个交易栏：3 绿宝石 -> 1 skill_crystal
                return new MerchantOffer(
                        new ItemStack(Items.EMERALD, 12), // 成本
                        new ItemStack(com.cceve.passivesystemskill.items.ModItems.SKILL_CRYSTAL.get()), // 奖励
                        64, // 最大交易次数
                        5, // 村民经验
                        0.05f // 价格浮动
                );
            });
        }
    }
}
