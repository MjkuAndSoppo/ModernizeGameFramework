package com.modernizegameframework.stamina;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 体力值系统 Mod 事件总线监听器
 * 处理需要在 Mod 事件总线上注册的能力与属性相关事件
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class StaminaModEvents {

    /**
     * 注册 Stamina 能力类型
     */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(Stamina.class);
    }

    /**
     * 将最大体力属性附加到玩家实体
     */
    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(net.minecraft.world.entity.EntityType.PLAYER, MaxStaminaAttribute.MAX_STAMINA.get());
    }
}
