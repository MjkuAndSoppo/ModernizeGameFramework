package com.modernizegameframework.inventory;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 塔科夫背包系统能力提供者
 * 将装备槽与扩展格能力附加到玩家实体
 */
public class TarkovInventoryProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    private final TarkovInventoryCapabilityImpl inventory = new TarkovInventoryCapabilityImpl();
    private final LazyOptional<TarkovInventoryCapability> optional = LazyOptional.of(() -> inventory);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return inventory.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        inventory.deserializeNBT(tag);
    }
}
