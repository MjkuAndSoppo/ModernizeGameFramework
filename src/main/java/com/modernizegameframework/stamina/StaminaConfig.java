package com.modernizegameframework.stamina;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 体力值系统的配置类
 * 包含各项消耗、恢复、阈值等可调参数
 */
public class StaminaConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 基础数值
    public static final ForgeConfigSpec.DoubleValue DEFAULT_MAX_STAMINA;
    public static final ForgeConfigSpec.DoubleValue STAMINA_PER_ROW;

    // 恢复速度
    public static final ForgeConfigSpec.DoubleValue REGEN_RATE;
    public static final ForgeConfigSpec.DoubleValue REGEN_DELAY;

    // 消耗数值
    public static final ForgeConfigSpec.DoubleValue SPRINT_COST;
    public static final ForgeConfigSpec.DoubleValue SPRINT_IDLE_COST;
    public static final ForgeConfigSpec.DoubleValue SWIM_COST;
    public static final ForgeConfigSpec.DoubleValue CLIMB_COST;
    public static final ForgeConfigSpec.DoubleValue JUMP_COST;
    public static final ForgeConfigSpec.DoubleValue ATTACK_COST;

    // 阈值
    public static final ForgeConfigSpec.DoubleValue SPEED_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SPRINT_LOCK_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue WEAK_THRESHOLD;

    static {
        BUILDER.push("stamina");

        BUILDER.comment("基础数值设置");
        DEFAULT_MAX_STAMINA = BUILDER
                .comment("玩家默认最大体力值")
                .defineInRange("defaultMaxStamina", 50.0, 1.0, 1024.0);
        STAMINA_PER_ROW = BUILDER
                .comment("每条体力条显示的体力值数量，超出则向上换行")
                .defineInRange("staminaPerRow", 50.0, 1.0, 1024.0);

        BUILDER.pop();
        BUILDER.push("regen");

        BUILDER.comment("自然恢复设置");
        REGEN_RATE = BUILDER
                .comment("每 tick 自然恢复的体力值")
                .defineInRange("regenRate", 0.15, 0.0, 1024.0);
        REGEN_DELAY = BUILDER
                .comment("消耗体力后多久开始恢复（单位：tick，20 tick = 1 秒）")
                .defineInRange("regenDelay", 20.0, 0.0, 10000.0);

        BUILDER.pop();
        BUILDER.push("cost");

        BUILDER.comment("各项行为消耗的体力值");
        SPRINT_COST = BUILDER
                .comment("疾跑时每 tick 消耗的体力")
                .defineInRange("sprintCost", 0.12, 0.0, 1024.0);
        SPRINT_IDLE_COST = BUILDER
                .comment("按住疾跑键但未真正进入疾跑状态时每 tick 消耗的体力")
                .defineInRange("sprintIdleCost", 0.06, 0.0, 1024.0);
        SWIM_COST = BUILDER
                .comment("游泳时每 tick 消耗的体力")
                .defineInRange("swimCost", 0.15, 0.0, 1024.0);
        CLIMB_COST = BUILDER
                .comment("攀爬时每 tick 消耗的体力")
                .defineInRange("climbCost", 0.1, 0.0, 1024.0);
        JUMP_COST = BUILDER
                .comment("每次跳跃消耗的体力")
                .defineInRange("jumpCost", 2.0, 0.0, 1024.0);
        ATTACK_COST = BUILDER
                .comment("每次攻击消耗的体力")
                .defineInRange("attackCost", 3.0, 0.0, 1024.0);

        BUILDER.pop();
        BUILDER.push("threshold");

        BUILDER.comment("阈值设置");
        SPEED_THRESHOLD = BUILDER
                .comment("水平移动速度超过此值时开始检测体力消耗（玩家步行约为 0.10，疾跑约为 0.13）")
                .defineInRange("speedThreshold", 0.08, 0.0, 10.0);
        SPRINT_LOCK_THRESHOLD = BUILDER
                .comment("体力低于等于此值时禁止开始疾跑，回复到高于此值后可再次疾跑")
                .defineInRange("sprintLockThreshold", 10.0, 0.0, 1024.0);
        WEAK_THRESHOLD = BUILDER
                .comment("体力百分比低于等于此值时视为低体力，跳跃高度降低且攻击伤害减少 25%（0.1 = 10%）")
                .defineInRange("weakThreshold", 0.1, 0.0, 1.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private StaminaConfig() {}
}
