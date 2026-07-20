package com.modernizegameframework;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // 肢节血量系统配置
    public static final ForgeConfigSpec.BooleanValue BODYPART_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BODYPART_HEALTH_BONUS;

    public static final ForgeConfigSpec.DoubleValue BODYPART_HEAD_RATIO;
    public static final ForgeConfigSpec.DoubleValue BODYPART_BODY_RATIO;
    public static final ForgeConfigSpec.DoubleValue BODYPART_LEFT_ARM_RATIO;
    public static final ForgeConfigSpec.DoubleValue BODYPART_RIGHT_ARM_RATIO;
    public static final ForgeConfigSpec.DoubleValue BODYPART_LEFT_LEG_RATIO;
    public static final ForgeConfigSpec.DoubleValue BODYPART_RIGHT_LEG_RATIO;

    public static final ForgeConfigSpec.DoubleValue BODYPART_BLEED_CHANCE;
    public static final ForgeConfigSpec.IntValue BODYPART_BLEED_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue BODYPART_BLEED_DAMAGE;
    public static final ForgeConfigSpec.IntValue BODYPART_BLEED_DURATION;

    public static final ForgeConfigSpec.IntValue BODYPART_PAIN_DURATION;
    public static final ForgeConfigSpec.IntValue BODYPART_PAINKILLER_DURATION;
    public static final ForgeConfigSpec.IntValue BODYPART_PAINKILLER_COOLDOWN;

    public static final ForgeConfigSpec.DoubleValue BODYPART_LEG_DESTROYED_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BODYPART_LEG_DESTROYED_SPEED_LIMIT;
    public static final ForgeConfigSpec.IntValue BODYPART_LEG_DESTROYED_STAMINA_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue BODYPART_LEG_DESTROYED_STAMINA_COST;

    public static final ForgeConfigSpec.IntValue BODYPART_ARM_WEAKNESS_MINING_FATIGUE_LEVEL;
    public static final ForgeConfigSpec.DoubleValue BODYPART_ARM_WEAKNESS_RELOAD_MULTIPLIER;

    static {
        BUILDER.push("bodypart");

        BUILDER.comment("肢节血量系统总开关");
        BODYPART_ENABLED = BUILDER
                .define("enabled", true);

        BUILDER.comment("血量上限加成");
        BODYPART_HEALTH_BONUS = BUILDER
                .comment("开启肢节血量系统后给玩家增加的最大生命值")
                .defineInRange("healthBonus", 68.0, 0.0, 1024.0);

        BUILDER.comment("各部位血量占总血量的比例（总和应接近 1.0，余数会自动补给躯干）");
        BODYPART_HEAD_RATIO = BUILDER
                .comment("头部血量比例")
                .defineInRange("headRatio", 0.12, 0.0, 1.0);
        BODYPART_BODY_RATIO = BUILDER
                .comment("躯干血量比例")
                .defineInRange("bodyRatio", 0.30, 0.0, 1.0);
        BODYPART_LEFT_ARM_RATIO = BUILDER
                .comment("左臂血量比例")
                .defineInRange("leftArmRatio", 0.15, 0.0, 1.0);
        BODYPART_RIGHT_ARM_RATIO = BUILDER
                .comment("右臂血量比例")
                .defineInRange("rightArmRatio", 0.15, 0.0, 1.0);
        BODYPART_LEFT_LEG_RATIO = BUILDER
                .comment("左腿血量比例")
                .defineInRange("leftLegRatio", 0.14, 0.0, 1.0);
        BODYPART_RIGHT_LEG_RATIO = BUILDER
                .comment("右腿血量比例")
                .defineInRange("rightLegRatio", 0.14, 0.0, 1.0);

        BUILDER.pop();
        BUILDER.push("bodypart.bleed");

        BUILDER.comment("出血机制设置");
        BODYPART_BLEED_CHANCE = BUILDER
                .comment("受击后触发出血的概率（0.0 ~ 1.0）")
                .defineInRange("chance", 0.10, 0.0, 1.0);
        BODYPART_BLEED_INTERVAL = BUILDER
                .comment("出血间隔（单位：tick，20 tick = 1 秒）")
                .defineInRange("interval", 60, 1, 10000);
        BODYPART_BLEED_DAMAGE = BUILDER
                .comment("每次出血造成的部位伤害")
                .defineInRange("damage", 1.0, 0.0, 1024.0);
        BODYPART_BLEED_DURATION = BUILDER
                .comment("出血持续时间（单位：tick）")
                .defineInRange("duration", 1200, 1, 100000);

        BUILDER.pop();
        BUILDER.push("bodypart.pain");

        BUILDER.comment("疼痛与止痛药设置");
        BODYPART_PAIN_DURATION = BUILDER
                .comment("疼痛效果持续时间（单位：tick）")
                .defineInRange("duration", 100, 1, 10000);
        BODYPART_PAINKILLER_DURATION = BUILDER
                .comment("止痛药屏蔽疼痛的持续时间（单位：tick）")
                .defineInRange("painkillerDuration", 2400, 1, 100000);
        BODYPART_PAINKILLER_COOLDOWN = BUILDER
                .comment("止痛药使用冷却（单位：tick）")
                .defineInRange("painkillerCooldown", 1200, 0, 100000);

        BUILDER.pop();
        BUILDER.push("bodypart.penalty");

        BUILDER.comment("部位黑掉后的惩罚设置");
        BODYPART_LEG_DESTROYED_SPEED_MULTIPLIER = BUILDER
                .comment("腿黑后移动速度乘数（0.7 = 降低 30%）")
                .defineInRange("legDestroyedSpeedMultiplier", 0.7, 0.0, 1.0);
        BODYPART_LEG_DESTROYED_SPEED_LIMIT = BUILDER
                .comment("腿黑后水平速度上限（单位：m/s，0 表示不限制）")
                .defineInRange("legDestroyedSpeedLimit", 2.0, 0.0, 100.0);
        BODYPART_LEG_DESTROYED_STAMINA_INTERVAL = BUILDER
                .comment("腿黑后行走扣体力的间隔（单位：tick）")
                .defineInRange("legDestroyedStaminaInterval", 30, 1, 10000);
        BODYPART_LEG_DESTROYED_STAMINA_COST = BUILDER
                .comment("腿黑后每次行走扣除的体力值")
                .defineInRange("legDestroyedStaminaCost", 1.0, 0.0, 1024.0);
        BODYPART_ARM_WEAKNESS_MINING_FATIGUE_LEVEL = BUILDER
                .comment("臂黑后挖掘疲劳等级（0 = 挖掘疲劳 I）")
                .defineInRange("armWeaknessMiningFatigueLevel", 0, 0, 10);
        BODYPART_ARM_WEAKNESS_RELOAD_MULTIPLIER = BUILDER
                .comment("臂黑后 TAC:Z 换弹速度乘数（0.5 = 降低 50%）")
                .defineInRange("armWeaknessReloadMultiplier", 0.5, 0.0, 1.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object obj)
    {
        if (!(obj instanceof final String itemName)) return false;
        final ResourceLocation location = ResourceLocation.tryParse(itemName);
        return location != null && ForgeRegistries.ITEMS.containsKey(location);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .map(ForgeRegistries.ITEMS::getValue)
                .collect(Collectors.toSet());
    }
}
