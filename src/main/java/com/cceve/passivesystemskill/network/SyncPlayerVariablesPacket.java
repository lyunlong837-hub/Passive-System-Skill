package com.cceve.passivesystemskill.network;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPlayerVariablesPacket {
    private final PlayerVariables data;

    public SyncPlayerVariablesPacket(PlayerVariables vars) {
        this.data = vars;
    }

    public SyncPlayerVariablesPacket(FriendlyByteBuf buf) {
        this.data = new PlayerVariables();
        this.data.loadNBT(buf.readNbt()); // 从 NBT 读
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(data.saveNBT()); // 存 NBT
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 第一步：延迟 1 tick
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;
                if (player == null)
                    return;

                // 第二步：再延迟 1 tick
                Minecraft.getInstance().execute(() -> {
                    player.getCapability(PlayerVariablesProvider.CAPABILITY).ifPresent(vars -> {
                        // 第三步：再延迟 1 tick
                        Minecraft.getInstance().execute(() -> {
                            vars.loadNBT(data.saveNBT()); // 最终更新客户端数据
                        });
                    });
                });
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
