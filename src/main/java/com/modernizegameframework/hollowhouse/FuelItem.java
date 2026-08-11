package com.modernizegameframework.hollowhouse;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 燃油物品
 * 用于供电站燃料，每点耐久对应 1 分钟发电值
 */
public class FuelItem extends Item {

    public FuelItem(Properties properties, int maxDamage) {
        super(properties.durability(maxDamage));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }
}
