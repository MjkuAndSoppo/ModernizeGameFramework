package com.modernizegameframework.securecontainer;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * Curios API 集成
 * 注册安全箱槽位，提供从 Curios 读取装备的安全箱物品的方法
 * 当 Curios 未安装时，所有方法安全返回空值
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SecureContainerCurios {

    /** Curios 安全箱槽位标识符 */
    public static final String SLOT_ID = "secure_container";

    /**
     * 向 Curios 注册安全箱槽位类型
     * 使用原版副手栏图案作为图标
     */
    @SubscribeEvent
    public static void enqueueIMC(final InterModEnqueueEvent event) {
        if (!ModList.get().isLoaded("curios")) return;
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE,
                () -> new SlotTypeMessage.Builder(SLOT_ID)
                        .icon(ResourceLocation.withDefaultNamespace("item/empty_armor_slot_shield"))
                        .size(1)
                        .build());
    }

    /**
     * 检测 Curios 是否已安装
     */
    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded("curios");
    }

    /**
     * 从 Curios 安全箱槽位获取装备的容器物品
     *
     * @param player 玩家
     * @return 装备的安全箱物品，未装备或 Curios 未安装时返回 EMPTY
     */
    public static ItemStack getCuriosSecureContainer(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try {
            Optional<SlotResult> result = CuriosApi.getCuriosInventory(player)
                    .resolve()
                    .flatMap(handler -> handler.findCurio(SLOT_ID, 0));
            return result.map(SlotResult::stack).orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}