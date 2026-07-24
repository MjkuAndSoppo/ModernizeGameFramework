package com.modernizegameframework.hollowhouse;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 藏身处数据能力提供者
 * 负责将 HollowHouseData 附加到玩家实体并支持自动序列化保存
 */
public class HollowHouseDataProvider implements ICapabilitySerializable<CompoundTag> {

    private final HollowHouseData data;
    private final LazyOptional<HollowHouseData> optional;

    public HollowHouseDataProvider() {
        this.data = new HollowHouseDataImpl();
        this.optional = LazyOptional.of(() -> data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return HollowHouseDataRegistry.HOLLOW_HOUSE_DATA_CAPABILITY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.deserializeNBT(tag);
    }
}
