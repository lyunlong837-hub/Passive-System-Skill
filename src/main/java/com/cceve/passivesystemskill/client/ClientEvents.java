package com.cceve.passivesystemskill.client;

import com.cceve.passivesystemskill.pss_main;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(modid = pss_main.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (ModKeyBindings.OPEN_SKILL_TREE.consumeClick()) {
            // 打开自定义界面
            Minecraft.getInstance().setScreen(new SkillTreeScreen());
        }
    }
}
