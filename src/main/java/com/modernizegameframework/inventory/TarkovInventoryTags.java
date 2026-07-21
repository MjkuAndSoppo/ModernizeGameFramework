package com.modernizegameframework.inventory;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 塔科夫背包系统使用的物品标签
 */
public class TarkovInventoryTags {

    /**
     * 胸挂装备标签：mfg:chest_rig
     */
    public static final TagKey<Item> CHEST_RIG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("mfg", "chest_rig"));

    /**
     * 背包装备标签：mfg:backpack
     */
    public static final TagKey<Item> BACKPACK = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("mfg", "backpack"));

    /**
     * 安全箱装备标签：mfg:secure_case
     */
    public static final TagKey<Item> SECURE_CASE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("mfg", "secure_case"));

    private TarkovInventoryTags() {
    }
}
