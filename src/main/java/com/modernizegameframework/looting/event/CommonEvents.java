package com.modernizegameframework.looting.event;

import com.modernizegameframework.looting.config.BetterLootingConfig;
import com.modernizegameframework.looting.config.BetterLootingConfig.PickupInterceptMode;
import com.modernizegameframework.looting.platform.PlatformHooks;

/**
 * 通用事件注册类
 * 负责在服务端和客户端共同运行的逻辑
 */
public class CommonEvents {

    /**
     * 初始化通用事件
     */
    public static void init() {
        BetterLootingConfig cfg = BetterLootingConfig.get();

        if (cfg.pickupInterceptMode == PickupInterceptMode.ALWAYS) {
            // 始终拦截：无条件阻止原版拾取，完全由模组接管
            // 实际拦截逻辑由 ItemEntityMixin.playerTouch 处理（判断 ALWAYS 模式并取消）
            PlatformHooks.setupPickupInterception();
        } else if (cfg.pickupInterceptMode == PickupInterceptMode.AUTO) {
            // 智能模式：使用 Forge 低优先级拦截器，仅在无其他模组处理时才拦截
            PlatformHooks.setupPickupInterception();
        }
    }
}