package com.cceve.passivesystemskill.capability;

import com.cceve.passivesystemskill.pss_main;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;

public class PlayerVariablesProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

    public static final Capability<PlayerVariables> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(pss_main.MODID,
            "player_variables");

    private final PlayerVariables data = new PlayerVariables();
    private final LazyOptional<PlayerVariables> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.saveNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.loadNBT(nbt);
    }

    /** 生命周期结束时可调用（一般不需要手动调） */
    public void invalidate() {
        optional.invalidate();
    }

    // ========= 建议优先使用下面这两个“安全”获取 =========

    /** 安全获取：可能为空 */
    public static Optional<PlayerVariables> getOptional(Entity e) {
        return e.getCapability(CAPABILITY).resolve();
    }

    /** 安全获取：没有就返回 null（不要在 Clone 里用会抛异常的 get） */
    public static PlayerVariables getOrNull(Entity e) {
        return e.getCapability(CAPABILITY).orElse(null);
    }

    /** 旧方法：缺失就抛异常 —— 不要在 Clone/Respawn 流程里用它 */
    @Deprecated
    public static PlayerVariables get(Entity e) {
        return e.getCapability(CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("PlayerVariables missing"));
    }
}
