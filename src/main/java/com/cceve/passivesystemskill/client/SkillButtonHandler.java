package com.cceve.passivesystemskill.client;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.config.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = pss_main.MODID, value = Dist.CLIENT)
public class SkillButtonHandler {

    @SubscribeEvent
    public static void onGuiInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            int x = screen.getGuiLeft() + Config.CLIENT.hudPosX.get();
            int y = screen.getGuiTop() + Config.CLIENT.hudPosY.get();
            boolean show = Config.CLIENT.showSkillHud.get();
            if (show) {
                // 在背包右上角放一个按钮
                Button btn = Button.builder(
                        Component.literal("技能"), // 按钮文字
                        (b) -> Minecraft.getInstance().setScreen(new SkillTreeScreen()) // 打开技能界面
                ).pos(x, y) // 位置：背包左上角 + 偏移
                        .size(40, 20) // 按钮大小
                        .build();

                event.addListener(btn);
            }
        }
    }
}
