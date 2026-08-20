package com.modernizegameframework.looting.client.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * JEI (Just Enough Items) 兼容性入口类。
 * 作为一个“软依赖”包装器，确保在玩家没有安装 JEI 时，模组依然可以正常运行而不会崩溃。
 */
public class JeiCompat {
    // 缓存 JEI 是否已加载的状态，避免在每帧渲染或高频事件中重复调用 ModList.isLoaded 产生性能开销。
    public static final boolean IS_JEI_LOADED = ModList.get().isLoaded("jei");

    /**
     * 获取玩家当前在 JEI 面板中鼠标悬停的物品。
     * 供外部调用的安全方法。
     *
     * @return 如果鼠标悬停在 JEI 物品上则返回该物品，否则返回 ItemStack.EMPTY
     */
    public static ItemStack getHoveredItem() {
        if (IS_JEI_LOADED) {
            return Internal.getHoveredItem();
        }
        return ItemStack.EMPTY;
    }

    /**
     * 内部类：物理隔离 JEI 的代码。
     * 当前构建环境未提供可用 JEI API，故 JEI 悬停拾取功能暂为禁用，
     * 统一返回空物品。若后续接入对应版本的 JEI API，可在此处恢复实现。
     */
    private static class Internal {
        static ItemStack getHoveredItem() {
            return ItemStack.EMPTY;
        }
    }
}