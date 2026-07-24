package com.modernizegameframework.hollowhouse;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 藏身处系统配置
 * 控制藏身处功能的总开关与各项参数
 */
public class HollowHouseConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec SPEC;

    // 藏身处系统总开关
    public static final ForgeConfigSpec.BooleanValue ENABLED;

    // 藏身处空间大小（以区块为单位，每区块 16 格）
    public static final ForgeConfigSpec.IntValue SIZE_CHUNKS;

    // 中央基础平台大小（格）
    public static final ForgeConfigSpec.IntValue PLATFORM_SIZE;

    // 邀请权限超时时间（分钟），0 表示持续到房主退出
    public static final ForgeConfigSpec.IntValue INVITE_TIMEOUT_MINUTES;

    static {
        BUILDER.push("hollowhouse");

        BUILDER.comment("藏身处系统总开关");
        ENABLED = BUILDER
                .define("enabled", true);

        BUILDER.comment("藏身处空间大小（以区块为单位）");
        SIZE_CHUNKS = BUILDER
                .comment("藏身处世界为 SIZE_CHUNKS × SIZE_CHUNKS 的方形区域")
                .defineInRange("sizeChunks", 2, 1, 8);

        BUILDER.comment("中央基础平台大小（格），建议为偶数");
        PLATFORM_SIZE = BUILDER
                .defineInRange("platformSize", 8, 4, 32);

        BUILDER.comment("邀请权限超时时间（分钟），0 表示持续到房主退出藏身处");
        INVITE_TIMEOUT_MINUTES = BUILDER
                .defineInRange("inviteTimeoutMinutes", 0, 0, 1440);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
