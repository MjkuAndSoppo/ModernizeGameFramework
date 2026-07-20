package com.modernizegameframework;

import com.mojang.logging.LogUtils;
import com.modernizegameframework.bodypart.BodyPartEffects;
import com.modernizegameframework.bodypart.BodyPartNetwork;
import com.modernizegameframework.medical.MedicalItems;
import com.modernizegameframework.movement.MovementConfig;
import com.modernizegameframework.movement.MovementNetwork;
import com.modernizegameframework.securecontainer.SecureContainerConfig;
import com.modernizegameframework.securecontainer.SecureContainerItem;
import com.modernizegameframework.securecontainer.SecureContainerNetwork;
import com.modernizegameframework.securecontainer.SecureContainerRegistry;
import com.modernizegameframework.securecontainer.SecureContainerScreen;
import com.modernizegameframework.stamina.MaxStaminaAttribute;
import com.modernizegameframework.stamina.StaminaConfig;
import com.modernizegameframework.stamina.StaminaNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ModernizeGameFramework.MODID)
public class ModernizeGameFramework
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "modernizegameframework";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "modernizegameframework" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "modernizegameframework" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "modernizegameframework" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "modernizegameframework:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "modernizegameframework:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    // Creates a new food item with the id "modernizegameframework:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build())));

    // Creates a creative tab with the id "modernizegameframework:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(MedicalItems.BANDAGE.get());
                output.accept(MedicalItems.BIG_BANDAGE.get());
                output.accept(MedicalItems.AI2_MEDKIT.get());
                output.accept(MedicalItems.IFAK.get());
                output.accept(MedicalItems.CMS_KIT.get());
                output.accept(MedicalItems.BIG_SURGICAL_KIT.get());
                output.accept(MedicalItems.PAINKILLER.get());
                output.accept(MedicalItems.BIG_PAINKILLER.get());
            }).build());

    public ModernizeGameFramework(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register medical items
        MedicalItems.ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register the max stamina attribute deferred register
        MaxStaminaAttribute.ATTRIBUTES.register(modEventBus);

        // Register body part status effects
        BodyPartEffects.EFFECTS.register(modEventBus);

        // Register stamina network messages
        StaminaNetwork.register();

        // Register body part network messages
        BodyPartNetwork.register();

        // Register movement network messages
        MovementNetwork.register();

        // Register secure container network messages
        SecureContainerNetwork.register();

        // Register secure container items, menu types, and creative tabs
        SecureContainerRegistry.ITEMS.register(modEventBus);
        SecureContainerRegistry.MENU_TYPES.register(modEventBus);
        SecureContainerRegistry.CREATIVE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register stamina config with a separate file name
        context.registerConfig(ModConfig.Type.COMMON, StaminaConfig.SPEC, "modernizegameframework-stamina.toml");

        // Register movement config with a separate file name
        context.registerConfig(ModConfig.Type.COMMON, MovementConfig.SPEC, "modernizegameframework-movement.toml");

        // Register secure container config with a separate file name
        context.registerConfig(ModConfig.Type.COMMON, SecureContainerConfig.SPEC, "modernizegameframework-securecontainer.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            // 注册安全箱界面
            net.minecraft.client.gui.screens.MenuScreens.register(
                    SecureContainerRegistry.SECURE_CONTAINER_MENU.get(), SecureContainerScreen::new);

            // 安全箱模型直接使用对应颜色的潜影盒模型，无需额外着色
        }
    }
}
