package com.cceve.passivesystemskill.registry;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.enchant.FasterExpGetEnchantment;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    private ModEnchantments() {
    }

    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(Registries.ENCHANTMENT,
            pss_main.MODID);

    public static final RegistryObject<Enchantment> FASTEREXPGET = ENCHANTMENTS.register("fasterexpget",
            FasterExpGetEnchantment::new);

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);

    }
}
