package com.modernizegameframework.securecontainer;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 安全箱系统 Forge 事件总线监听器
 * 负责能力附加到玩家、死亡克隆保留数据
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID)
public class SecureContainerEvents {

    /**
     * 为玩家实体附加安全箱库存能力
     */
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            SecureContainerProvider provider = new SecureContainerProvider();
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "secure_container"),
                    provider);
        }
    }

    /**
     * 玩家死亡/切换维度时保留安全箱数据，并在服务端同步给客户端
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        original.reviveCaps();

        original.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(oldInv -> {
            newPlayer.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(newInv -> {
                newInv.deserializeNBT(oldInv.serializeNBT());
            });
        });

        original.invalidateCaps();

        // 将复制后的数据立即同步到客户端，确保重生后 overlay 能直接显示
        if (!newPlayer.level().isClientSide && newPlayer instanceof ServerPlayer serverPlayer) {
            SecureContainerNetwork.syncAll(serverPlayer);
        }
    }
}