package com.cceve.passivesystemskill.commands;

import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.cceve.passivesystemskill.config.Config;

@Mod.EventBusSubscriber(modid = pss_main.MODID)
public class PssCommands {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
                CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

                dispatcher.register(
                                Commands.literal("pss")
                                                .requires(source -> source.hasPermission(2)) // 需要 OP
                                                .then(Commands.literal("point")
                                                                .then(Commands.argument("target",
                                                                                EntityArgument.player())
                                                                                // give
                                                                                .then(Commands.literal("give")
                                                                                                .then(Commands.argument(
                                                                                                                "amount",
                                                                                                                IntegerArgumentType
                                                                                                                                .integer(1))
                                                                                                                .executes(ctx -> {
                                                                                                                        ServerPlayer target = EntityArgument
                                                                                                                                        .getPlayer(ctx, "target");
                                                                                                                        int amount = IntegerArgumentType
                                                                                                                                        .getInteger(ctx, "amount");
                                                                                                                        return addPoints(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        target,
                                                                                                                                        amount);
                                                                                                                })))
                                                                                // set
                                                                                .then(Commands.literal("set")
                                                                                                .then(Commands.argument(
                                                                                                                "amount",
                                                                                                                IntegerArgumentType
                                                                                                                                .integer(0))
                                                                                                                .executes(ctx -> {
                                                                                                                        ServerPlayer target = EntityArgument
                                                                                                                                        .getPlayer(ctx, "target");
                                                                                                                        int amount = IntegerArgumentType
                                                                                                                                        .getInteger(ctx, "amount");
                                                                                                                        return setPoints(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        target,
                                                                                                                                        amount);
                                                                                                                })))
                                                                                // min
                                                                                .then(Commands.literal("min")
                                                                                                .then(Commands.argument(
                                                                                                                "amount",
                                                                                                                IntegerArgumentType
                                                                                                                                .integer(1))
                                                                                                                .executes(ctx -> {
                                                                                                                        ServerPlayer target = EntityArgument
                                                                                                                                        .getPlayer(ctx, "target");
                                                                                                                        int amount = IntegerArgumentType
                                                                                                                                        .getInteger(ctx, "amount");
                                                                                                                        return removePoints(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        target,
                                                                                                                                        amount);
                                                                                                                })))

                                                                                // query
                                                                                .then(Commands.literal("query")
                                                                                                .executes(ctx -> {
                                                                                                        ServerPlayer target = EntityArgument
                                                                                                                        .getPlayer(ctx, "target");
                                                                                                        return queryPoints(
                                                                                                                        ctx.getSource(),
                                                                                                                        target);
                                                                                                })))));
        }

        private static int addPoints(CommandSourceStack source, ServerPlayer target, int amount) {
                target.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(v -> {
                        v.currencies.skillPoints += amount;
                        target.sendSystemMessage(
                                        Component.literal("你获得了 " + amount + " 点技能点，当前共 " + v.currencies.skillPoints));
                        source.sendSuccess(
                                        () -> Component.literal(
                                                        "已给 " + target.getName().getString() + " " + amount + " 点技能点"),
                                        true);
                });
                return 1;
        }

        private static int setPoints(CommandSourceStack source, ServerPlayer target, int amount) {
                target.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(v -> {
                        v.currencies.skillPoints = amount;
                        target.sendSystemMessage(Component.literal("你的技能点已被设置为 " + v.currencies.skillPoints));
                        source.sendSuccess(
                                        () -> Component.literal(
                                                        "已将 " + target.getName().getString() + " 的技能点设置为 " + amount),
                                        true);
                });
                return 1;
        }

        private static int removePoints(CommandSourceStack source, ServerPlayer target, int amount) {
                target.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(v -> {
                        v.currencies.skillPoints = Math.max(0, v.currencies.skillPoints - amount);
                        target.sendSystemMessage(
                                        Component.literal("你被扣除了 " + amount + " 点技能点，当前共 " + v.currencies.skillPoints));
                        source.sendSuccess(() -> Component
                                        .literal("已扣除 " + target.getName().getString() + " 的 " + amount + " 点技能点"),
                                        true);
                });
                return 1;
        }

        private static int queryPoints(CommandSourceStack source, ServerPlayer target) {
                target.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(v -> {
                        source.sendSuccess(() -> Component.literal(
                                        target.getName().getString() + " 当前有 " + v.currencies.skillPoints
                                                        + " 点技能点,全局倍率为: " + Config.SERVER.exp_global_config.get()),
                                        false);
                });
                return 1;
        }
}
