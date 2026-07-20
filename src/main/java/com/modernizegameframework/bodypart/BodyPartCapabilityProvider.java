package com.modernizegameframework.bodypart;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 肢节血量能力提供者
 * 负责将 BodyPartCapability 能力附加到玩家实体上
 */
public class BodyPartCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    private final BodyPartCapabilityImpl bodyPartCapability;
    private final LazyOptional<BodyPartCapability> optional;

    public BodyPartCapabilityProvider(Player player) {
        this.bodyPartCapability = new BodyPartCapabilityImpl(player);
        this.optional = LazyOptional.of(() -> bodyPartCapability);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return BodyPartCapabilityRegistry.BODY_PART_CAPABILITY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return bodyPartCapability.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        bodyPartCapability.deserializeNBT(tag);
    }
}
