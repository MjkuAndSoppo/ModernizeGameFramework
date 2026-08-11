package com.modernizegameframework.hollowhouse;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 藏身处工作方块类型
 * 定义可在控制箱 UI 中解锁与升级的工作方块
 */
public enum HollowHouseWorkBlockType {

    /**
     * 仓库
     */
    STOREHOUSE("storehouse", "仓库", "no", 4, 0, new int[]{3, 5, 9}),

    /**
     * 医疗站
     */
    MEDICAL("medical", "医疗站", "yes", 3, 15, new int[]{15, 20}),

    /**
     * 供电站
     */
    POWER("power", "供电站", "no", 3, 20, new int[]{10, 15}),

    /**
     * 照明
     */
    LIGHTING("lighting", "照明", "yes", 3, 15, new int[]{15, 20});

    private final String id;
    private final String displayName;
    private final String requiresPower;
    private final int maxLevel;
    private final int unlockCost;
    private final int[] upgradeCosts;

    HollowHouseWorkBlockType(String id, String displayName, String requiresPower, int maxLevel, int unlockCost, int[] upgradeCosts) {
        this.id = id;
        this.displayName = displayName;
        this.requiresPower = requiresPower;
        this.maxLevel = maxLevel;
        this.unlockCost = unlockCost;
        this.upgradeCosts = upgradeCosts;
    }

    /**
     * 获取工作方块 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取是否需要供电站发电状态
     * "yes" 表示需要供电，"no" 表示不需要
     */
    public String getRequiresPower() {
        return requiresPower;
    }

    /**
     * 判断是否需要供电站发电状态
     */
    public boolean isRequiresPower() {
        return "yes".equalsIgnoreCase(requiresPower);
    }

    /**
     * 获取最大等级
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * 获取解锁所需经验等级（从锁定到 1 级）
     */
    public int getUnlockCost() {
        return unlockCost;
    }

    /**
     * 获取升级到指定等级所需经验等级
     *
     * @param level 目标等级，从 1 开始；1 表示解锁
     */
    public int getUpgradeCost(int level) {
        if (level <= 1) {
            return unlockCost;
        }
        if (level > maxLevel) {
            return 0;
        }
        return upgradeCosts[level - 2];
    }

    /**
     * 根据 ID 查找工作方块类型
     */
    @Nullable
    public static HollowHouseWorkBlockType fromId(String id) {
        for (HollowHouseWorkBlockType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取所有工作方块类型
     */
    public static Map<String, HollowHouseWorkBlockType> getAll() {
        Map<String, HollowHouseWorkBlockType> map = new HashMap<>();
        for (HollowHouseWorkBlockType type : values()) {
            map.put(type.id, type);
        }
        return map;
    }
}
