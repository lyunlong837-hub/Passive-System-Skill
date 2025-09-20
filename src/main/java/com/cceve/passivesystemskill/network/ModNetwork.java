package com.cceve.passivesystemskill.network;

import java.util.function.Supplier;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariables.PlayerSkills;
import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.liandong.iron_spell.ISSAttribute;
import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(pss_main.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static int id = 0;

    private static int nextId() {
        return id++;
    }

    public static void register() {
        CHANNEL.registerMessage(nextId(), S2C_SyncPlayerVars.class,
                S2C_SyncPlayerVars::encode, S2C_SyncPlayerVars::decode, S2C_SyncPlayerVars::handle);

        CHANNEL.registerMessage(nextId(), C2S_RequestFullSync.class,
                C2S_RequestFullSync::encode, C2S_RequestFullSync::decode, C2S_RequestFullSync::handle);

        CHANNEL.registerMessage(nextId(), C2S_ToggleNode.class,
                C2S_ToggleNode::encode, C2S_ToggleNode::decode, C2S_ToggleNode::handle);
    }

    /** 服务端 -> 客户端：整块同步玩家变量 */
    public static void syncToClient(ServerPlayer player, PlayerVariables vars) {
        CHANNEL.sendTo(new S2C_SyncPlayerVars(vars), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    /* ====================== S2C：整块同步 ====================== */
    public static class S2C_SyncPlayerVars {
        public PlayerVariables data = new PlayerVariables();

        public S2C_SyncPlayerVars() {
        }

        public S2C_SyncPlayerVars(PlayerVariables src) {
            this.data.copyFrom(src);
        }

        public static void encode(S2C_SyncPlayerVars msg, FriendlyByteBuf buf) {
            buf.writeNbt(msg.data.saveNBT());
        }

        public static S2C_SyncPlayerVars decode(FriendlyByteBuf buf) {
            S2C_SyncPlayerVars m = new S2C_SyncPlayerVars();
            if (buf.readableBytes() > 0) {
                var tag = buf.readNbt();
                if (tag != null)
                    m.data.loadNBT(tag);
            }
            return m;
        }

        public static void handle(S2C_SyncPlayerVars msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                var mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.getCapability(PlayerVariablesProvider.CAPABILITY)
                            .ifPresent(vars -> vars.copyFrom(msg.data));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* ====================== C2S：请求整块同步（例如界面打开时） ===================== */
    public static class C2S_RequestFullSync {
        public C2S_RequestFullSync() {
        }

        public static void encode(C2S_RequestFullSync msg, FriendlyByteBuf buf) {
        }

        public static C2S_RequestFullSync decode(FriendlyByteBuf buf) {
            return new C2S_RequestFullSync();
        }

        public static void handle(C2S_RequestFullSync msg, Supplier<NetworkEvent.Context> ctx) {
            var c = ctx.get();
            c.enqueueWork(() -> {
                ServerPlayer sp = c.getSender();
                if (sp == null)
                    return;
                var vars = PlayerVariablesProvider.get(sp);
                ModNetwork.syncToClient(sp, vars);
            });
            c.setPacketHandled(true);
        }
    }

    /* ====================== C2S：升级/降级 1 级 ===================== */
    public static class C2S_ToggleNode {
        public SkillId id;
        public boolean upgrade; // true=升级(+1)，false=降级(-1)
        public int unusedCost; // 兼容旧字段，不使用

        public C2S_ToggleNode() {
        }

        public C2S_ToggleNode(SkillId id, boolean upgrade, int unusedCost) {
            this.id = id;
            this.upgrade = upgrade;
            this.unusedCost = unusedCost;
        }

        public static void encode(C2S_ToggleNode msg, FriendlyByteBuf buf) {
            buf.writeEnum(msg.id);
            buf.writeBoolean(msg.upgrade);
            buf.writeVarInt(msg.unusedCost);
        }

        public static C2S_ToggleNode decode(FriendlyByteBuf buf) {
            return new C2S_ToggleNode(buf.readEnum(SkillId.class), buf.readBoolean(), buf.readVarInt());
        }

        public static void handle(C2S_ToggleNode msg, Supplier<NetworkEvent.Context> ctx) {
            var c = ctx.get();
            c.enqueueWork(() -> {
                ServerPlayer sp = c.getSender();
                if (sp == null)
                    return;

                // 数据
                PlayerVariables vars = PlayerVariablesProvider.get(sp);
                PlayerSkills map = vars.skillMap;

                var def = SkillTreeLibrary.node(msg.id);
                if (def == null) {
                    toast(sp, "未知技能");
                    return;
                }

                // 取记录；升级时若为 null 将自动创建
                PlayerVariables.SkillRecord rec = (map == null) ? null : map.get(msg.id);
                int cur = (rec == null) ? 0 : rec.level;

                int maxLv = def.maxLevel;
                int cost = def.costPerLevel;

                if (msg.upgrade) {
                    // ========== 升级 ==========
                    if (cur >= maxLv) {
                        toast(sp, "已满级");
                        return;
                    }

                    // 仅在“解锁(0→1)”时检查父节点要求
                    boolean unlocking = (cur <= 0);
                    if (unlocking && !parentsSatisfiedServer(vars, msg.id)) {
                        toast(sp, "前置未满足");
                        return;
                    }

                    if (vars.currencies.skillPoints < cost) {
                        toast(sp, "技能点不足");
                        return;
                    }

                    // 扣点 + 升级（缺记录则创建）
                    if (rec == null) {
                        rec = new PlayerVariables.SkillRecord();
                        rec.level = 0;
                        map.put(msg.id, rec);
                    }
                    vars.currencies.skillPoints -= cost;
                    rec.level = cur + 1;

                    toast(sp, "升级成功：%s → Lv.%d".formatted(def.name, rec.level));

                } else {
                    // ========== 降级 ==========
                    if (cur <= 0) {
                        toast(sp, "已是0级");
                        return;
                    }

                    // 简单策略：允许降级，并全额退回本级花费
                    rec.level = cur - 1;
                    vars.currencies.skillPoints += cost;

                    toast(sp, "已降级：%s → Lv.%d".formatted(def.name, rec.level));
                }

                // 重算属性（安全版本，避免空指针）
                com.cceve.passivesystemskill.runtime.SkillAttributeApplier.applyAllSafe(sp);
                com.cceve.passivesystemskill.liandong.iron_spell.ISSAttribute.applyAllSafe(sp);
                // 同步回客户端
                ModNetwork.syncToClient(sp, vars);
            });
            c.setPacketHandled(true);
        }

        // 伪代码：放在 C2S_ToggleNode 处理里
        private static boolean parentsSatisfiedServer(PlayerVariables vars, SkillId childId) {
            if (vars == null)
                return false;
            for (SkillId par : SkillTreeLibrary.parentsOf(childId)) {
                int need = SkillTreeLibrary.requiredParentLevel(par, childId);
                var rec = vars.skillMap.get(par);
                int have = (rec == null ? 0 : rec.level);
                if (have < need)
                    return false;
            }
            return true;
        }

        private static void toast(ServerPlayer sp, String text) {
            sp.displayClientMessage(Component.literal(text), true);
        }
    }
}
