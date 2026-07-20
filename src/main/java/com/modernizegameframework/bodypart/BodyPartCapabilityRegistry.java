package com.modernizegameframework.bodypart;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * 肢节血量能力注册中心
 * 负责创建 BodyPartCapability 能力的 Capability 实例
 */
public class BodyPartCapabilityRegistry {

    /**
     * 玩家肢节血量能力实例
     */
    public static final Capability<BodyPartCapability> BODY_PART_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private BodyPartCapabilityRegistry() {}
}
