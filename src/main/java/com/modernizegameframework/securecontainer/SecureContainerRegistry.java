package com.modernizegameframework.securecontainer;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 安全箱系统注册中心
 * 负责注册物品、能力、菜单类型和创造标签页
 */
public class SecureContainerRegistry {

    /** 安全箱库存能力实例 */
    public static final Capability<SecureContainerInventory> SECURE_CONTAINER_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    /** 物品注册器 */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ModernizeGameFramework.MODID);

    /** 菜单类型注册器 */
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ModernizeGameFramework.MODID);

    /** 创造模式标签页注册器 */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModernizeGameFramework.MODID);

    // === 五种安全箱物品 ===

    public static final RegistryObject<Item> ZENER = ITEMS.register("secure_container_zener",
            () -> new SecureContainerItem(SecureContainerType.ZENER));

    public static final RegistryObject<Item> OUGAS = ITEMS.register("secure_container_ougas",
            () -> new SecureContainerItem(SecureContainerType.OUGAS));

    public static final RegistryObject<Item> GAMMA = ITEMS.register("secure_container_gamma",
            () -> new SecureContainerItem(SecureContainerType.GAMMA));

    public static final RegistryObject<Item> DELTA = ITEMS.register("secure_container_delta",
            () -> new SecureContainerItem(SecureContainerType.DELTA));

    public static final RegistryObject<Item> EPSILON = ITEMS.register("secure_container_epsilon",
            () -> new SecureContainerItem(SecureContainerType.EPSILON));

    // === 菜单类型 ===

    public static final RegistryObject<MenuType<SecureContainerMenu>> SECURE_CONTAINER_MENU =
            MENU_TYPES.register("secure_container_menu",
                    () -> IForgeMenuType.create(SecureContainerMenu::new));

    // === 创造模式标签页 ===

    public static final RegistryObject<CreativeModeTab> SECURE_CONTAINER_TAB = CREATIVE_TABS.register(
            "secure_container_tab",
            () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable(
                            "itemGroup.modernizegameframework.secure_container"))
                    .icon(() -> new net.minecraft.world.item.ItemStack(ZENER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ZENER.get());
                        output.accept(OUGAS.get());
                        output.accept(GAMMA.get());
                        output.accept(DELTA.get());
                        output.accept(EPSILON.get());
                    })
                    .build());

    private SecureContainerRegistry() {}
}