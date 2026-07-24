package com.modernizegameframework.hollowhouse;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * 藏身处玩家数据能力注册中心
 */
public class HollowHouseDataRegistry {

    /**
     * 藏身处数据能力实例
     */
    public static final Capability<HollowHouseData> HOLLOW_HOUSE_DATA_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private HollowHouseDataRegistry() {}
}
