package com.cceve.passivesystemskill.config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ServerConfig SERVER;

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        final Pair<ServerConfig, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder()
                .configure(ServerConfig::new);
        SERVER_SPEC = serverPair.getRight();
        SERVER = serverPair.getLeft();

        final Pair<ClientConfig, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder()
                .configure(ClientConfig::new);
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    public static class ServerConfig {
        public final ForgeConfigSpec.DoubleValue exp_global_config;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("skills");
            exp_global_config = builder
                    .comment("全局经验加成 (默认 1 = 100%)")
                    .defineInRange("exp_global_config", 1.0, 0.0, 9999.0);
            builder.pop();
        }
    }

    public static class ClientConfig {
        public final ForgeConfigSpec.BooleanValue showSkillHud;
        public final ForgeConfigSpec.IntValue hudPosX;
        public final ForgeConfigSpec.IntValue hudPosY;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("背包按钮");

            showSkillHud = builder
                    .comment("是否显示背包技能按钮")
                    .define("showSkillHud", true);

            hudPosX = builder
                    .comment("HUD 按钮 X 坐标偏移")
                    .defineInRange("hudPosX", 130, -10000, 10000);

            hudPosY = builder
                    .comment("HUD 按钮 Y 坐标偏移")
                    .defineInRange("hudPosY", -20, -10000, 10000);

            builder.pop();
        }
    }
}
