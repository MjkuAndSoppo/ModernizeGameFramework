package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 藏身处系统 Mod 事件总线监听器
 * 处理需要在 Mod 事件总线上注册的能力相关事件
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HollowHouseModEvents {

    /**
     * 注册藏身处数据能力类型
     */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(HollowHouseData.class);
    }
}
