package com.cceve.passivesystemskill;

import com.cceve.passivesystemskill.items.ModItems;
import com.cceve.passivesystemskill.liandong.tetra.WorkbenchCraftEvent;
import com.cceve.passivesystemskill.tabs.ModCreativeTabs;
import com.cceve.passivesystemskill.network.ModNetwork;
import com.cceve.passivesystemskill.registry.ModEffects;
import com.cceve.passivesystemskill.registry.ModEnchantments;
import com.cceve.passivesystemskill.registry.ModPotions;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.cceve.passivesystemskill.config.Config;
import net.minecraftforge.fml.config.ModConfig;

@Mod(pss_main.MODID)
public final class pss_main {
    public static final String MODID = "passivesystemskill";

    public pss_main() {
        // Forge 1.20.1 正确拿法：无参构造 + 这里取 modEventBus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // DeferredRegister 注册到 modEventBus
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // 注册网络（你也可以放到 FMLCommonSetupEvent 里）
        ModNetwork.register();

        ModEnchantments.register(modEventBus);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);

        // 服务端配置 (服务器/单机保存到 serverconfig 文件夹)
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        // 客户端配置 (只存在于客户端，每个玩家本地保存到 config 文件夹)
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);

        // 如果还有别的注册（方块、菜单、数据生成等），都在这里用同一个 bus 调用
        // ModBlocks.register(modEventBus); ...

        if (ModList.get().isLoaded("irons_spellbooks")) {
            // 注册和 Spellbooks 的联动内容
        }
        // 注册和 tetra 的联动内容
        if (ModList.get().isLoaded("tetra")) {
            MinecraftForge.EVENT_BUS.register(WorkbenchCraftEvent.class);
        }

    }
}
