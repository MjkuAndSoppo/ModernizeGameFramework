package com.modernizegameframework.stamina;

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
 * 体力值能力提供者
 * 负责将 Stamina 能力附加到玩家实体上
 */
public class StaminaProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    private final StaminaCapability stamina;
    private final LazyOptional<Stamina> optional;

    public StaminaProvider(Player player) {
        this.stamina = new StaminaCapability(player);
        this.optional = LazyOptional.of(() -> stamina);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return StaminaRegistry.STAMINA_CAPABILITY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return stamina.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        stamina.deserializeNBT(tag);
    }
}
