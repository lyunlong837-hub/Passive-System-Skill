package com.cceve.passivesystemskill.tabs;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, pss_main.MODID);

    public static final RegistryObject<CreativeModeTab> PSS_TAB = CREATIVE_MODE_TABS.register("pss_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.passivesystemskill.pss_tab"))
                    .icon(() -> new ItemStack(ModItems.SKILL_CRYSTAL.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.SKILL_CRYSTAL.get());
                        output.accept(ModItems.SKILL_CRYSTAL_PIECE.get());
                        // 这里把你想放进物品栏的条目都加上
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private ModCreativeTabs() {
    }
}
