package com.modernizegameframework.bodypart;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 肢节血量系统 Mod 事件总线监听器
 * 处理需要在 Mod 事件总线上注册的能力相关事件
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BodyPartModEvents {

    /**
     * 注册 BodyPartCapability 能力类型
     */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(BodyPartCapability.class);
    }
}
