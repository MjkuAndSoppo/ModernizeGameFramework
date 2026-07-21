package com.modernizegameframework.inventory;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 塔科夫背包系统能力注册与事件
 * 负责注册能力实例、附加到玩家以及死亡/跨维度时保留数据
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID)
public class TarkovInventoryCapabilityRegistry {

    /** 塔科夫背包能力实例 */
    public static final Capability<TarkovInventoryCapability> TARKOV_INVENTORY_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    /**
     * 为玩家实体附加塔科夫背包能力
     */
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            TarkovInventoryProvider provider = new TarkovInventoryProvider();
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "tarkov_inventory"),
                    provider);
        }
    }

    /**
     * 玩家死亡或切换维度时保留装备槽与安全箱内容，扩展格物品通过死亡掉落事件处理
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        original.reviveCaps();

        original.getCapability(TARKOV_INVENTORY_CAPABILITY).ifPresent(oldInv -> {
            newPlayer.getCapability(TARKOV_INVENTORY_CAPABILITY).ifPresent(newInv -> {
                CompoundTag keep = new CompoundTag();
                keep.put("Equipment", oldInv.getEquipmentInventory().serializeNBT());
                keep.put("Secure", oldInv.getSecureInventory().serializeNBT());
                newInv.deserializeNBT(keep);
            });
        });

        original.invalidateCaps();
    }

    /**
     * 玩家死亡时将扩展格物品加入掉落物列表，装备槽与安全箱内容保留
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(TARKOV_INVENTORY_CAPABILITY).ifPresent(inv -> {
            ItemStackHandler expansion = inv.getExpansionInventory();
            for (int i = 0; i < expansion.getSlots(); i++) {
                ItemStack stack = expansion.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy());
                    itemEntity.setDefaultPickUpDelay();
                    event.getDrops().add(itemEntity);
                }
            }
            inv.clearExpansion();
        });
    }

    private TarkovInventoryCapabilityRegistry() {
    }
}
