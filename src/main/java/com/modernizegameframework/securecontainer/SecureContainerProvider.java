package com.modernizegameframework.securecontainer;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 安全箱库存能力提供者
 * 负责将 SecureContainerInventory 能力附加到玩家实体上
 */
public class SecureContainerProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    private final SecureContainerInventoryImpl inventory;
    private final LazyOptional<SecureContainerInventory> optional;

    public SecureContainerProvider() {
        this.inventory = new SecureContainerInventoryImpl();
        this.optional = LazyOptional.of(() -> inventory);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY.orEmpty(cap, optional);
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