package com.cceve.passivesystemskill.items;

import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.text.DecimalFormat;

public class SkillCrystalItem extends Item {

    public SkillCrystalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // 整叠数量
            int count = stack.getCount();
            if (count <= 0) {
                return InteractionResultHolder.fail(stack);
            }

            final double addPer = 1; // 每个宝石增加的技能点
            final int cooldownTicks = 5; // 可选冷却（0 取消）

            player.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(v -> {
                double before = v.currencies.skillPoints;
                double totalAdd = addPer * count; // 总加成 = 每个 * 数量
                v.currencies.skillPoints = before + totalAdd;

                // 消耗整叠
                if (!player.isCreative()) {
                    stack.shrink(count);
                }

                // 可选冷却
                if (cooldownTicks > 0) {
                    player.getCooldowns().addCooldown(this, cooldownTicks);
                }

                // 提示消息
                java.text.DecimalFormat df = new java.text.DecimalFormat("0");
                player.displayClientMessage(
                        Component.literal("技能点 +" + df.format(totalAdd) + " → " + df.format(before + totalAdd))
                                .withStyle(ChatFormatting.AQUA),
                        true);
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}