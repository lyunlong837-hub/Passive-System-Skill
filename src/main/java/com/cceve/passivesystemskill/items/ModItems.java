package com.cceve.passivesystemskill.items;

import com.cceve.passivesystemskill.pss_main;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus; // ← 新增
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
                        pss_main.MODID);

        public static final RegistryObject<Item> SKILL_CRYSTAL = ITEMS.register("skill_crystal",
                        () -> new SkillCrystalItem(new Item.Properties().stacksTo(64)));
        public static final RegistryObject<Item> SKILL_CRYSTAL_PIECE = ITEMS.register("skill_crystal_piece",
                        () -> new SkillCrystalItem(new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> IRONSPELL_ZWF = ITEMS.register("ironspell_zwf",
                        () -> new SkillCrystalItem(new Item.Properties().stacksTo(64)));
        public static final RegistryObject<Item> IRONSPELL_ZWFB = ITEMS.register("ironspell_zwfb",
                        () -> new SkillCrystalItem(new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> TETRA_ZWF = ITEMS.register("tetra_zwf",
                        () -> new SkillCrystalItem(new Item.Properties().stacksTo(64)));
        public static final RegistryObject<Item> TETRA_ZWFB = ITEMS.register("tetra_zwfb",
                        () -> new SkillCrystalItem(new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> RY_ZWF = ITEMS.register("ry_zwf",
                        () -> new SkillCrystalItem(new Item.Properties().stacksTo(64)));

        /** 由主 Mod 把 IEventBus 传进来 */
        public static void register(IEventBus modEventBus) {
                ITEMS.register(modEventBus);
        }

        private ModItems() {
        }
}
