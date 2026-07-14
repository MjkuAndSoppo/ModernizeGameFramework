package com.modernizegameframework.stamina;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * 体力值能力的注册中心
 * 负责创建 Stamina 能力的 Capability 实例
 */
public class StaminaRegistry {

    /**
     * 玩家体力值能力实例
     */
    public static final Capability<Stamina> STAMINA_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private StaminaRegistry() {}
}
