package com.cceve.passivesystemskill.client;

import org.lwjgl.glfw.GLFW;

import com.cceve.passivesystemskill.pss_main;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = pss_main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyBindings {

    public static KeyMapping OPEN_SKILL_TREE;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        OPEN_SKILL_TREE = new KeyMapping(
                "key.passivesystemskill.open_skill_tree", // 键位ID（lang里翻译）
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V, // 默认按键 V
                "key.categories.passivesystemskill" // 分类
        );
        event.register(OPEN_SKILL_TREE);
    }
}
