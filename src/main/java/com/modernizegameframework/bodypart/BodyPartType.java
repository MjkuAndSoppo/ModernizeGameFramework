package com.modernizegameframework.bodypart;

/**
 * 人体部位枚举
 * 定义塔科夫式肢节血量系统的所有部位及其基础属性
 */
public enum BodyPartType {
    HEAD("head", 0.12f, true),
    BODY("body", 0.30f, true),
    LEFT_ARM("larm", 0.15f, false),
    RIGHT_ARM("rarm", 0.15f, false),
    LEFT_LEG("lleg", 0.14f, false),
    RIGHT_LEG("rleg", 0.14f, false);

    private final String id;
    private final float baseRatio;
    private final boolean vital;

    BodyPartType(String id, float baseRatio, boolean vital) {
        this.id = id;
        this.baseRatio = baseRatio;
        this.vital = vital;
    }

    /**
     * 获取部位唯一标识符
     */
    public String getId() {
        return id;
    }

    /**
     * 获取部位基础血量占比
     */
    public float getBaseRatio() {
        return baseRatio;
    }

    /**
     * 判断是否为致命部位（头或躯干）
     */
    public boolean isVital() {
        return vital;
    }

    /**
     * 根据英文缩写查找部位
     *
     * @param id 部位缩写
     * @return 部位枚举，找不到返回 null
     */
    public static BodyPartType fromId(String id) {
        for (BodyPartType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
