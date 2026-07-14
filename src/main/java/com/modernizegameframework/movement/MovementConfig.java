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

    // 连跳
    public static final ForgeConfigSpec.BooleanValue AUTO_BHOP;
    public static final ForgeConfigSpec.DoubleValue BHOP_COST;
    public static final ForgeConfigSpec.BooleanValue BHOP_KEEP_SPEED;
    public static final ForgeConfigSpec.DoubleValue BHOP_ACCUMULATE_RATE;
    public static final ForgeConfigSpec.DoubleValue BHOP_BONUS_MAX_SPEED;

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
        BHOP_ACCUMULATE_RATE = BUILDER
                .comment("连跳速度累加率（每次连跳落地前速度的此比例累加到下次连跳基础速度，0.1 = 10%）")
                .defineInRange("bhopAccumulateRate", 0.1, 0.0, 1.0);
        BHOP_BONUS_MAX_SPEED = BUILDER
                .comment("连跳累加奖励速度上限（m/s），奖励标量达到此值后不再增加")
                .defineInRange("bhopBonusMaxSpeed", 15.0, 0.0, 100.0);

        BUILDER.comment("体力联动设置");
        DEPLETED_FRICTION = BUILDER
                .comment("体力归零时落地强制摩擦系数（每 tick 水平速度乘以此值，越小减速越猛）")
                .defineInRange("depletedFriction", 0.3, 0.0, 1.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private MovementConfig() {}
}
