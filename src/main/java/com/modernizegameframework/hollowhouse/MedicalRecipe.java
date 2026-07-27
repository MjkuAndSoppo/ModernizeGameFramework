package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.medical.MedicalItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 医疗站配方
 * 每个枚举定义一种可在医疗站中生产的医疗物品
 * 格式：{名称}, {医疗站等级}, {消耗物品列表}, {经验点消耗}, {生产时间（秒）}
 */
public enum MedicalRecipe {

    /**
     * 绷带
     */
    BANDAGE("绷带", 1,
            List.of(new ItemStack(Items.STRING, 2)),
            new ItemStack(MedicalItems.BANDAGE.get()),
            0, 5),

    /**
     * 止痛药
     */
    PAINKILLER("止痛药", 1,
            List.of(new ItemStack(Items.WHEAT_SEEDS, 1), new ItemStack(Items.BONE, 1)),
            new ItemStack(MedicalItems.PAINKILLER.get()),
            0, 10),

    /**
     * 军用绷带
     */
    BIG_BANDAGE("军用绷带", 2,
            List.of(new ItemStack(MedicalItems.BANDAGE.get(), 1), new ItemStack(Items.STRING, 2)),
            new ItemStack(MedicalItems.BIG_BANDAGE.get()),
            5, 10),

    /**
     * AI-2 急救组合
     */
    AI2_MEDKIT("AI-2急救组合", 2,
            List.of(new ItemStack(Items.APPLE, 1), new ItemStack(Items.REDSTONE, 1)),
            new ItemStack(MedicalItems.AI2_MEDKIT.get()),
            5, 15),

    /**
     * CMS 手术包
     */
    CMS_KIT("CMS手术包", 3,
            List.of(new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.GOLD_INGOT, 1)),
            new ItemStack(MedicalItems.CMS_KIT.get()),
            10, 30);

    private final String displayName;
    private final int requiredLevel;
    private final List<ItemStack> ingredients;
    private final ItemStack output;
    private final int experienceCost;
    private final int productionSeconds;

    MedicalRecipe(String displayName, int requiredLevel,
                  List<ItemStack> ingredients, ItemStack output,
                  int experienceCost, int productionSeconds) {
        this.displayName = displayName;
        this.requiredLevel = requiredLevel;
        // 复制一份不可变列表，避免外部修改
        this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
        this.output = output.copy();
        this.experienceCost = experienceCost;
        this.productionSeconds = productionSeconds;
    }

    /**
     * 获取配方显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取所需医疗站等级
     */
    public int getRequiredLevel() {
        return requiredLevel;
    }

    /**
     * 获取消耗物品列表（只读）
     */
    public List<ItemStack> getIngredients() {
        return ingredients;
    }

    /**
     * 获取产出物品
     */
    public ItemStack getOutput() {
        return output.copy();
    }

    /**
     * 获取每次制作消耗的经验点数
     */
    public int getExperienceCost() {
        return experienceCost;
    }

    /**
     * 获取生产时间（秒）
     */
    public int getProductionSeconds() {
        return productionSeconds;
    }

    /**
     * 判断指定等级是否可以制作本配方
     *
     * @param medicalLevel 医疗站当前等级
     */
    public boolean isAvailable(int medicalLevel) {
        return medicalLevel >= requiredLevel;
    }

    /**
     * 根据名称查找配方
     */
    public static MedicalRecipe fromName(String name) {
        for (MedicalRecipe recipe : values()) {
            if (recipe.name().equalsIgnoreCase(name)) {
                return recipe;
            }
        }
        return null;
    }
}
