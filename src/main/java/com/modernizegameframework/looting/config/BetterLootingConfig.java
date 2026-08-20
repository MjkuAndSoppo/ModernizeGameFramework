package com.modernizegameframework.looting.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;

/**
 * 模组的核心配置类，负责管理"更好拾取"（BetterLooting）功能的用户偏好设置。
 * 仅保留核心功能设置与状态持久化；视觉 / 判定参数已按需求写死为固定值。
 * 使用 NightConfig 库生成带注释的 TOML 文件。
 */
public class BetterLootingConfig {
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("modernizegameframework-looting.toml").toFile();
    // 旧的 JSON 配置文件，用于更新时自动清理
    private static final File OLD_CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("better_looting.json").toFile();

    // ==========================================
    // 核心功能设置 (Core Feature Settings)
    // ==========================================
    /** 战利品拾取功能总开关（可由 /mgf looting on|off 切换并持久化） */
    public boolean enabled = true;
    public PickupInterceptMode pickupInterceptMode = PickupInterceptMode.AUTO;
    public LongPressMode longPressMode = LongPressMode.PICKUP_ALL;
    public int stabilityThresholdTicks = 4;
    public boolean enableRareItemFilter = true;
    /** 超大堆叠自动合并：按需求固定关闭，仅保留"额外数量"计数机制供批量拾取使用 */
    public final boolean enableSuperMerge = false;

    // ==========================================
    // 状态持久化设置 (Persistent State Settings)
    // ==========================================
    public FilterMode lastFilterMode = FilterMode.ALL;
    public boolean lastAutoMode = false;

    // ==========================================
    // 写死的判定参数 (Fixed Parameters)
    // 以下字段仅保留 getter 供逻辑层读取，已由需求确定为固定值，不再写入配置文件。
    // ==========================================
    /** 水平拾取检测范围 */
    public final float scanRangeXZ = 1.5f;
    /** 垂直拾取检测范围 */
    public final float scanRangeY = 1.5f;
    /** HUD 触发角度（已无 HUD，仅作逻辑保留） */
    public final float lookDownAngle = 45.0f;
    /** 拾取延迟保护 (秒) */
    public final float pickupDelaySeconds = 0.2f;
    /** 长按触发时间 (ticks = 1秒) */
    public final int maxHoldTicks = 20;
    /** 超大堆叠合并范围 (已禁用，保留字段) */
    public final float mergeRangeXZ = 5.0f;
    public final float mergeRangeY = 5.0f;
    /** 运输方块黑名单 (已禁用，保留字段) */
    public final String mergeTransportBlacklist = "belt,conveyor,chute,funnel,depot";
    /** 物品栏掉落列表布局：写死的固定值 */
    public final boolean showInventoryLootList = true;
    public final int inventoryListWidth = 100;
    public final float inventoryListXOffset = 0.0f;
    public final float inventoryListYOffset = 0.0f;
    public final float inventoryListScale = 1.0f;
    public final float inventoryListAlpha = 0.9f;
    public final int inventoryListHeight = 166;
    /** 列表中单页可显示行数 */
    public final float visibleRows = 4.5f;
    /** HUD 激活模式：固定为始终激活 */
    public final ActivationMode activationMode = ActivationMode.ALWAYS;
    /** 滚轮模式：固定为始终滚动 */
    public final ScrollMode scrollMode = ScrollMode.ALWAYS;
    /** 动画速度：固定为中速 */
    public final AnimationSpeed animationSpeed = AnimationSpeed.MEDIUM;

    /**
     * 获取默认水平拾取范围。
     */
    public float getActualScanRangeXZ() {
        return serverScanRangeXZ > 0 ? serverScanRangeXZ : scanRangeXZ;
    }

    /**
     * 获取默认垂直拾取范围。
     */
    public float getActualScanRangeY() {
        return serverScanRangeY > 0 ? serverScanRangeY : scanRangeY;
    }

    /** 服务端强制覆盖参数（保留用于兼容，当前不会被下发） */
    public transient float serverScanRangeXZ = -1.0f;
    public transient float serverScanRangeY = -1.0f;

    /**
     * 触发物品拾取逻辑的条件模式
     */
    public enum ActivationMode { ALWAYS, LOOK_DOWN, STAND_STILL, KEY_HOLD, KEY_TOGGLE }

    /**
     * 允许在物品列表中滚动选择的条件模式
     */
    public enum ScrollMode { ALWAYS, KEY_BIND, INVERT_KEY, STAND_STILL }

    /**
     * 动画速度模式（写死为 MEDIUM）
     */
    public enum AnimationSpeed { SLOW, MEDIUM, FAST, OFF }

    /**
     * 原版拾取事件拦截策略
     */
    public enum PickupInterceptMode {
        /** 智能模式：仅在其他模组未处理拾取事件时才拦截（推荐，兼容性最好） */
        AUTO,
        /** 始终拦截：无条件阻止原版拾取（可能与背包类模组冲突） */
        ALWAYS
    }

    /**
     * 长按拾取行为模式
     */
    public enum LongPressMode {
        /** 全部拾取：长按一键拾取范围内所有掉落物 */
        PICKUP_ALL,
        /** 单行全部拾取：长按一键拾取当前选中行同类物品的全部 */
        PICKUP_ROW
    }

    private static BetterLootingConfig INSTANCE = new BetterLootingConfig();
    public static BetterLootingConfig get() { return INSTANCE; }

    /**
     * 使用 NightConfig 序列化配置并写入注释。
     * 仅写入核心功能开关与状态持久化字段，判定参数固定不写。
     */
    public static void save() {
        try (CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_FILE)
                .sync()
                .preserveInsertionOrder()
                .build()) {

            // --- 核心功能 ---
            config.setComment("Core", "核心功能设置 (Core Feature Settings)");
            config.setComment("Core.enabled", "战利品拾取功能总开关 (可由 /mgf looting on|off 切换)");
            config.set("Core.enabled", INSTANCE.enabled);
            config.setComment("Core.pickupInterceptMode", "拾取拦截策略: AUTO(智能,推荐) / ALWAYS(始终拦截)");
            config.set("Core.pickupInterceptMode", INSTANCE.pickupInterceptMode.name());
            config.setComment("Core.longPressMode", "长按拾取模式: PICKUP_ALL(全部拾取,默认) / PICKUP_ROW(单行全部拾取)");
            config.set("Core.longPressMode", INSTANCE.longPressMode.name());
            config.setComment("Core.enableRareItemFilter", "白名单是否启用默认稀有物品过滤。关闭后仅显示白名单内的物品");
            config.set("Core.enableRareItemFilter", INSTANCE.enableRareItemFilter);

            // --- 状态持久化 (不加注释，以免玩家误改) ---
            config.set("State.lastFilterMode", INSTANCE.lastFilterMode.name());
            config.set("State.lastAutoMode", INSTANCE.lastAutoMode);

            config.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取 TOML 配置，若损坏或不存在则重建。
     */
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            INSTANCE = new BetterLootingConfig();
            save();
            return;
        }

        try (CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_FILE).sync().build()) {
            config.load();

            INSTANCE.enabled = config.getOrElse("Core.enabled", true);
            try { INSTANCE.pickupInterceptMode = PickupInterceptMode.valueOf(config.getOrElse("Core.pickupInterceptMode", "AUTO")); } catch (Exception ignored) {}
            try { INSTANCE.longPressMode = LongPressMode.valueOf(config.getOrElse("Core.longPressMode", "PICKUP_ALL")); } catch (Exception ignored) {}
            INSTANCE.enableRareItemFilter = config.getOrElse("Core.enableRareItemFilter", true);

            try { INSTANCE.lastFilterMode = FilterMode.valueOf(config.getOrElse("State.lastFilterMode", "ALL")); } catch (Exception ignored) {}
            INSTANCE.lastAutoMode = config.getOrElse("State.lastAutoMode", false);
        } catch (Exception e) {
            e.printStackTrace();
            INSTANCE = new BetterLootingConfig();
            save();
        }
    }

    public static void init() {
        // 清理旧版本的 JSON 配置文件
        if (OLD_CONFIG_FILE.exists()) {
            try {
                if (OLD_CONFIG_FILE.delete()) {
                    System.out.println("[BetterLooting] Successfully deleted old JSON config file.");
                }
            } catch (Exception e) {
                System.err.println("[BetterLooting] Failed to delete old JSON config.");
                e.printStackTrace();
            }
        }
        load();
    }
}