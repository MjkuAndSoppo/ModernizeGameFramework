package com.modernizegameframework.securecontainer;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 安全箱系统配置
 */
public class SecureContainerConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    /** 安全箱系统总开关 */
    public static final ForgeConfigSpec.BooleanValue ENABLED;

    /** 附加面板与物品栏的间距（像素） */
    public static final ForgeConfigSpec.IntValue OVERLAY_GAP;

    /** 附加面板背景颜色（ARGB） */
    public static final ForgeConfigSpec.IntValue OVERLAY_BG_COLOR;

    static {
        BUILDER.push("secureContainer");

        ENABLED = BUILDER
                .comment("安全箱系统总开关，设为 false 可禁用整个安全箱功能")
                .define("enabled", true);

        OVERLAY_GAP = BUILDER
                .comment("附加面板与物品栏之间的间距（像素）")
                .defineInRange("overlayGap", 4, 0, 20);

        OVERLAY_BG_COLOR = BUILDER
                .comment("附加面板背景颜色（ARGB 格式，如 0xC0101010）")
                .defineInRange("overlayBgColor", 0xC0101010, 0, Integer.MAX_VALUE);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private SecureContainerConfig() {}
}