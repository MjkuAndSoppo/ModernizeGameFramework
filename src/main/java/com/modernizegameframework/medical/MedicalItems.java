package com.modernizegameframework.medical;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 医疗物品注册
 */
public class MedicalItems {

    /**
     * 医疗物品注册器
     */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModernizeGameFramework.MODID);

    // 消耗品
    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage",
            () -> new MedicalItem(new Item.Properties().stacksTo(16), -1, 20, 0, MedicalEffects.BANDAGE));

    public static final RegistryObject<Item> PAINKILLER = ITEMS.register("painkiller",
            () -> new MedicalItem(new Item.Properties().stacksTo(16), -1, 20, 0, MedicalEffects.PAINKILLER));

    // 耐久型
    public static final RegistryObject<Item> BIG_BANDAGE = ITEMS.register("big_bandage",
            () -> new MedicalItem(new Item.Properties().stacksTo(1), 4, 10, 20, MedicalEffects.BIG_BANDAGE));

    public static final RegistryObject<Item> AI2_MEDKIT = ITEMS.register("ai2_medkit",
            () -> new MedicalItem(new Item.Properties().stacksTo(1), 20, 40, 10, MedicalEffects.AI2_MEDKIT));

    public static final RegistryObject<Item> IFAK = ITEMS.register("ifak",
            () -> new MedicalItem(new Item.Properties().stacksTo(1), 20, 40, 10, MedicalEffects.IFAK));

    public static final RegistryObject<Item> CMS_KIT = ITEMS.register("cms_kit",
            () -> new MedicalItem(new Item.Properties().stacksTo(1), 3, 100, 20, MedicalEffects.CMS_KIT));

    public static final RegistryObject<Item> BIG_SURGICAL_KIT = ITEMS.register("big_surgical_kit",
            () -> new MedicalItem(new Item.Properties().stacksTo(1), 9, 40, 100, MedicalEffects.BIG_SURGICAL_KIT));

    public static final RegistryObject<Item> BIG_PAINKILLER = ITEMS.register("big_painkiller",
            () -> new MedicalItem(new Item.Properties().stacksTo(1), 12, 20, 20, MedicalEffects.BIG_PAINKILLER));

    private MedicalItems() {
    }
}
