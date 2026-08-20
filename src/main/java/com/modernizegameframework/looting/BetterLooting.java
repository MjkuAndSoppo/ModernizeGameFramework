package com.modernizegameframework.looting;

import com.modernizegameframework.looting.config.BetterLootingConfig;
import com.modernizegameframework.looting.event.CommonEvents;
import com.modernizegameframework.looting.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 战利品提升（BetterLooting）功能的公共入口。
 * 负责初始化双端（客户端+服务端）通用的设置、网络和事件。
 * 由主模组 {@code ModernizeGameFramework} 在初始化阶段调用。
 */
public class BetterLooting {
    public static final String MODID = "better_looting";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void init() {
        // 1. 初始化配置文件 (自动删除旧json)
        BetterLootingConfig.init();

        // 2. 注册网络通道与数据包
        NetworkHandler.register();

        // 3. 注册通用事件监听器
        CommonEvents.init();

        LOGGER.info("Better Looting (MGF) Common Initialized.");
    }
}