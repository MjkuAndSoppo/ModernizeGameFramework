package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 藏身处系统注册中心
 * 负责注册方块、物品等
 * 维度类型与维度定义通过数据包 JSON 注册
 */
public class HollowHouseRegistry {

    // 方块注册器
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ModernizeGameFramework.MODID);

    // 物品注册器
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ModernizeGameFramework.MODID);

    // 藏身处入口方块
    public static final RegistryObject<Block> HOLLOW_HOUSE_PORTAL = BLOCKS.register("hollow_house_portal",
            () -> new HollowHousePortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // 藏身处入口物品
    public static final RegistryObject<Item> HOLLOW_HOUSE_PORTAL_ITEM = ITEMS.register("hollow_house_portal",
            () -> new BlockItem(HOLLOW_HOUSE_PORTAL.get(), new Item.Properties()));

    // 控制箱方块
    public static final RegistryObject<Block> CONTROL_BOX = BLOCKS.register("control_box",
            () -> new HollowHouseControlBoxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(1.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // 控制箱物品
    public static final RegistryObject<Item> CONTROL_BOX_ITEM = ITEMS.register("control_box",
            () -> new BlockItem(CONTROL_BOX.get(), new Item.Properties()));

    private HollowHouseRegistry() {}
}
