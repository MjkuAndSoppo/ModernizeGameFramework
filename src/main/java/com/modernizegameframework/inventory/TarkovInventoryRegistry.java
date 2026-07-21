package com.modernizegameframework.inventory;

import com.modernizegameframework.ModernizeGameFramework;
import com.modernizegameframework.securecontainer.SecureContainerRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 塔科夫背包系统注册中心
 * 负责注册胸挂、背包物品，菜单类型和创造模式标签页
 */
public class TarkovInventoryRegistry {

    /** 物品注册器 */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ModernizeGameFramework.MODID);

    /** 菜单类型注册器 */
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ModernizeGameFramework.MODID);

    /** 创造模式标签页注册器 */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModernizeGameFramework.MODID);

    // === 胸挂等级物品（1~6级） ===

    public static final RegistryObject<Item> CHEST_RIG_LEVEL_1 = registerChestRig("chest_rig_level_1");
    public static final RegistryObject<Item> CHEST_RIG_LEVEL_2 = registerChestRig("chest_rig_level_2");
    public static final RegistryObject<Item> CHEST_RIG_LEVEL_3 = registerChestRig("chest_rig_level_3");
    public static final RegistryObject<Item> CHEST_RIG_LEVEL_4 = registerChestRig("chest_rig_level_4");
    public static final RegistryObject<Item> CHEST_RIG_LEVEL_5 = registerChestRig("chest_rig_level_5");
    public static final RegistryObject<Item> CHEST_RIG_LEVEL_6 = registerChestRig("chest_rig_level_6");

    // === 背包容量物品（小/中/大） ===

    public static final RegistryObject<Item> SMALL_BACKPACK = registerBackpack("small_backpack");
    public static final RegistryObject<Item> MEDIUM_BACKPACK = registerBackpack("medium_backpack");
    public static final RegistryObject<Item> LARGE_BACKPACK = registerBackpack("large_backpack");

    // === 安全箱物品（使用 SecureContainerRegistry 注册的 5 种类型） ===
    // secure_container_alpha / beta / gamma / kappa / theta 已在 SecureContainerRegistry 中注册

    // === 菜单类型 ===

    public static final RegistryObject<MenuType<TarkovInventoryMenu>> TARKOV_INVENTORY_MENU =
            MENU_TYPES.register("tarkov_inventory_menu",
                    () -> IForgeMenuType.create(TarkovInventoryMenu::new));

    // === 创造模式标签页 ===

    public static final RegistryObject<CreativeModeTab> EQUIPMENT_TAB = CREATIVE_TABS.register(
            "equipment_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.modernizegameframework.equipment"))
                    .icon(() -> CHEST_RIG_LEVEL_6.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(CHEST_RIG_LEVEL_1.get());
                        output.accept(CHEST_RIG_LEVEL_2.get());
                        output.accept(CHEST_RIG_LEVEL_3.get());
                        output.accept(CHEST_RIG_LEVEL_4.get());
                        output.accept(CHEST_RIG_LEVEL_5.get());
                        output.accept(CHEST_RIG_LEVEL_6.get());
                        output.accept(SMALL_BACKPACK.get());
                        output.accept(MEDIUM_BACKPACK.get());
                        output.accept(LARGE_BACKPACK.get());
                        output.accept(SecureContainerRegistry.ALPHA.get());
                        output.accept(SecureContainerRegistry.BETA.get());
                        output.accept(SecureContainerRegistry.GAMMA.get());
                        output.accept(SecureContainerRegistry.KAPPA.get());
                        output.accept(SecureContainerRegistry.THETA.get());
                    })
                    .build());

    private static RegistryObject<Item> registerChestRig(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1)));
    }

    private static RegistryObject<Item> registerBackpack(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1)));
    }

    private TarkovInventoryRegistry() {
    }
}
