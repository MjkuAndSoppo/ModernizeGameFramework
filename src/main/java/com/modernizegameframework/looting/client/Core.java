package com.modernizegameframework.looting.client;

import com.modernizegameframework.looting.client.core.InputGuard;
import com.modernizegameframework.looting.client.core.pipeline.*;
import com.modernizegameframework.looting.client.core.policy.ActivationPolicy;
import com.modernizegameframework.looting.client.core.policy.ModeManager;
import com.modernizegameframework.looting.client.filter.FilterWhitelist;
import com.modernizegameframework.looting.config.BetterLootingConfig;
import com.modernizegameframework.looting.config.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端核心门面，负责串联各子模块：扫描、模式、输入、拾取编排。
 * 单例模式。对外接口保持不变。
 */
public class Core {
    public static final Core INSTANCE = new Core();

    private final PickupHandler pickupHandler = new PickupHandler();
    private final SelectionManager selectionManager = new SelectionManager();
    private final KeyTracker keyTracker = new KeyTracker();

    private Core() {}

    public void init() {
        FilterWhitelist.INSTANCE.init();
        com.modernizegameframework.looting.client.skin.SkinManager.INSTANCE.init();
        ModeManager.INSTANCE.init();
        InputGuard.INSTANCE.init(selectionManager, keyTracker, pickupHandler);

        // 注册 Forge 客户端 Tick 事件（替换原 Architectury CLIENT_PRE）
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) return;
        onClientTick(mc);
    }

    public boolean isHudActive() {
        return ActivationPolicy.isHudActive(selectionManager, keyTracker);
    }

    public boolean shouldIgnoreScroll() {
        return ActivationPolicy.shouldIgnoreScroll(selectionManager, keyTracker);
    }

    public void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        selectionManager.updateItems(LootScanner.scan(mc, ModeManager.INSTANCE.getFilterMode()));

        boolean isPhysicalDown = keyTracker.isPhysicalKeyDown(KeyInit.PICKUP)
                || keyTracker.isPhysicalKeyDown(KeyInit.PICKUP_ALT);
        InputGuard.INSTANCE.tick(isPhysicalDown);

        keyTracker.tickOverlayToggle();

        // 自动拾取用全量未过滤列表判空，确保不被 StabilityFilter 延迟
        if (ModeManager.INSTANCE.isAutoMode() && !selectionManager.getUnfilteredItems().isEmpty()) {
            if (pickupHandler.canAutoPickup()) {
                ActionDispatcher.handleAutoPickup(selectionManager, pickupHandler);
            }
        } else {
            pickupHandler.resetAutoCooldown();
        }

        handleInputLogic();
    }

    private void handleInputLogic() {
        keyTracker.tickActionToggles(ModeManager.INSTANCE::toggleAutoMode);

        boolean isKeyDown = keyTracker.isPhysicalKeyDown(KeyInit.PICKUP)
                || keyTracker.isPhysicalKeyDown(KeyInit.PICKUP_ALT);
        boolean hasTargets = isHudActive();

        var action = pickupHandler.tickInput(isKeyDown, hasTargets);
        int delayTicks = (int) (BetterLootingConfig.get().pickupDelaySeconds * 20);

        switch (action) {
            case SINGLE -> {
                ActionDispatcher.sendSinglePickup(selectionManager);
                InputGuard.INSTANCE.setGraceTicks(delayTicks);
            }
            case BATCH -> {
                var mode = BetterLootingConfig.get().longPressMode;
                if (mode == BetterLootingConfig.LongPressMode.PICKUP_ROW) {
                    ActionDispatcher.sendRowPickup(selectionManager);
                } else {
                    List<ItemEntity> all = new ArrayList<>();
                    // 使用未过滤的全量数据，确保长按拾取不被 StabilityFilter 延迟
                    selectionManager.getUnfilteredItems().forEach(e -> all.addAll(e.getSourceEntities()));
                    ActionDispatcher.sendBatchPickup(all, false);
                }
                InputGuard.INSTANCE.setGraceTicks(delayTicks);
            }
        }
    }

    public void performScroll(double delta) {
        selectionManager.performScroll(delta);
    }

    public static boolean shouldIntercept() {
        return InputGuard.INSTANCE.shouldIntercept();
    }

    public FilterMode getFilterMode() { return ModeManager.INSTANCE.getFilterMode(); }
    public boolean isAutoMode() { return ModeManager.INSTANCE.isAutoMode(); }
    public List<VisualItemEntry> getNearbyItems() { return selectionManager.getNearbyItems(); }
    public int getSelectedIndex() { return selectionManager.getSelectedIndex(); }
    public int getTargetScrollOffset() { return selectionManager.getTargetScrollOffset(); }
    public float getPickupProgress() { return pickupHandler.getProgress(); }

    public boolean isItemInInventory(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (var stack : mc.player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) return true;
        }
        return false;
    }
}
