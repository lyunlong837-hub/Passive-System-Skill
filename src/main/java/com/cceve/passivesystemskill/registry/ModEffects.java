package com.cceve.passivesystemskill.registry;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.effect.FasterExpXgEffect;
import com.cceve.passivesystemskill.effect.MagicPowerEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    private ModEffects() {
    }

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT,
            pss_main.MODID);

    /** 药水效果 ID: fasterexpxg */
    public static final RegistryObject<MobEffect> FASTEREXPXG = MOB_EFFECTS.register("fasterexpxg",
            FasterExpXgEffect::new);

    public static final RegistryObject<MobEffect> MAGIC_POWER = MOB_EFFECTS.register("pss_magic_power",
            MagicPowerEffect::new);

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}
