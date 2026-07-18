package com.modernizegameframework.movement;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 惯性移动系统配置类
 * 包含地面惯性、空中加速、连跳等可调参数
 */
public class MovementConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 总开关
    public static final ForgeConfigSpec.BooleanValue ENABLED;

    // 地面惯性
    public static final ForgeConfigSpec.DoubleValue GROUND_FRICTION;
    public static final ForgeConfigSpec.DoubleValue GROUND_ACCEL;

    // 空中加速
    public static final ForgeConfigSpec.DoubleValue AIR_ACCEL;
    public static final ForgeConfigSpec.DoubleValue AIR_WISH_SPEED;
    public static final ForgeConfigSpec.DoubleValue AIR_CONTROL;

    // 急停惩罚
    public static final ForgeConfigSpec.DoubleValue STOP_SPEED_THRESHOLD_ANGLE;
    public static final ForgeConfigSpec.DoubleValue STOP_SPEED_MAX_PENALTY;

    // 连跳
    public static final ForgeConfigSpec.BooleanValue AUTO_BHOP;
    public static final ForgeConfigSpec.DoubleValue BHOP_COST;
    public static final ForgeConfigSpec.BooleanValue BHOP_KEEP_SPEED;

    // 体力联动
    public static final ForgeConfigSpec.DoubleValue DEPLETED_FRICTION;

    static {
        BUILDER.push("movement");

        BUILDER.comment("惯性移动系统总开关");
        ENABLED = BUILDER
                .define("enabled", true);

        BUILDER.comment("地面惯性设置");
        GROUND_FRICTION = BUILDER
                .comment("松键减速系数（1.0=无摩擦，越小越滑，原版约 0.6）")
                .defineInRange("groundFriction", 0.8, 0.0, 1.0);
        GROUND_ACCEL = BUILDER
                .comment("起步加速度（每 tick 增加的速度比例）")
                .defineInRange("groundAccel", 0.15, 0.0, 1.0);

        BUILDER.comment("空中加速设置（起源引擎风格）");
        AIR_ACCEL = BUILDER
                .comment("空中加速度系数（sv_airaccelerate 等效值），越高转向加速越强")
                .defineInRange("airAccel", 100.0, 0.0, 10000.0);
        AIR_WISH_SPEED = BUILDER
                .comment("空中期望速度上限，限制单次加速能达到的最大速度")
                .defineInRange("airWishSpeed", 0.3, 0.0, 10.0);
        AIR_CONTROL = BUILDER
                .comment("空中控制力（0=无法改向，1=完全控制），影响 wishSpeed 的实际使用比例")
                .defineInRange("airControl", 1.0, 0.0, 1.0);

        BUILDER.comment("急停惩罚设置（起源引擎 stopSpeed 机制）");
        STOP_SPEED_THRESHOLD_ANGLE = BUILDER
                .comment("急停惩罚触发角度（度），当前速度与按键朝向夹角超过此值开始减速")
                .defineInRange("stopSpeedThresholdAngle", 90.0, 0.0, 180.0);
        STOP_SPEED_MAX_PENALTY = BUILDER
                .comment("急停惩罚最大比例（0.5 = 最多减少 50% 速度），在 180° 时达到最大值")
                .defineInRange("stopSpeedMaxPenalty", 0.5, 0.0, 1.0);

        BUILDER.comment("连跳设置");
        AUTO_BHOP = BUILDER
                .comment("是否启用自动连跳（按住空格落地即连跳，无需精准按键）")
                .define("autoBhop", true);
        BHOP_COST = BUILDER
                .comment("连跳成功时消耗的体力值百分比（0.5 = 消耗最大体力的 0.5%）")
                .defineInRange("bhopCost", 0.5, 0.0, 100.0);
        BHOP_KEEP_SPEED = BUILDER
                .comment("连跳时是否保持水平速度（跳过地面摩擦衰减）")
                .define("bhopKeepSpeed", true);

        BUILDER.comment("体力联动设置");
        DEPLETED_FRICTION = BUILDER
                .comment("体力归零时落地强制摩擦系数（每 tick 水平速度乘以此值，越小减速越猛）")
                .defineInRange("depletedFriction", 0.3, 0.0, 1.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private MovementConfig() {}
}
