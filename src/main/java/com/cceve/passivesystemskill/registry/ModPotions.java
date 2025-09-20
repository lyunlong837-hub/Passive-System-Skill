package com.cceve.passivesystemskill.registry;

import com.cceve.passivesystemskill.pss_main;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModPotions {
    private ModPotions() {
    }

    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, pss_main.MODID);

    public static final RegistryObject<Potion> FASTEREXPXG = POTIONS.register("fasterexpxg",
            () -> new Potion(new MobEffectInstance(ModEffects.FASTEREXPXG.get(), 20 * 60 * 5, 0)));

    public static final RegistryObject<Potion> FASTEREXPXG_LONG = POTIONS.register("fasterexpxg_long",
            () -> new Potion(new MobEffectInstance(ModEffects.FASTEREXPXG.get(), 20 * 60 * 15, 0)));

    public static final RegistryObject<Potion> FASTEREXPXG_STRONG = POTIONS.register("fasterexpxg_strong",
            () -> new Potion(new MobEffectInstance(ModEffects.FASTEREXPXG.get(), 20 * 60 * 3, 1)));

    public static void register(IEventBus bus) {
        POTIONS.register(bus);
    }
}
