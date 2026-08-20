package com.modernizegameframework.looting.client;

import com.modernizegameframework.looting.client.core.InputGuard;
import com.modernizegameframework.looting.client.core.pipeline.*;
import com.modernizegameframework.looting.client.core.policy.ActivationPolicy;
import com.modernizegameframework.looting.client.core.policy.ModeManager;
import com.modernizegameframework.looting.client.filter.FilterWhitelist;
import com.modernizegameframework.looting.config.BetterLootingConfig;
import com.modernizegameframework.looting.config.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        handleInputLogic(mc);
    }

    private void handleInputLogic(Minecraft mc) {
        keyTracker.tickActionToggles(ModeManager.INSTANCE::toggleAutoMode);

        boolean isKeyDown = keyTracker.isPhysicalKeyDown(KeyInit.PICKUP)
                || keyTracker.isPhysicalKeyDown(KeyInit.PICKUP_ALT);
        boolean hasTargets = isHudActive();

        var action = pickupHandler.tickInput(isKeyDown, hasTargets);
        int delayTicks = (int) (BetterLootingConfig.get().pickupDelaySeconds * 20);

        switch (action) {
            case SINGLE -> {
                // 纯准心瞄准：只拾取准心对准的那个掉落物（不可隔墙，命中碰撞箱取最近者）
                VisualItemEntry aimed = resolveAimedTarget(mc);
                if (aimed != null) {
                    ActionDispatcher.sendEntryPickup(aimed);
                    InputGuard.INSTANCE.setGraceTicks(delayTicks);
                }
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

    /**
     * 准心瞄准拾取：以玩家视线发射射线，精确命中范围内掉落物的碰撞箱，
     * 取距离视线最近且未被方块遮挡（不可隔墙拾取）的一个视觉项返回。
     * @param mc 客户端实例
     * @return 被准心对准的视觉列表项；未命中返回 null
     */
    private VisualItemEntry resolveAimedTarget(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;

        // 拾取范围在 1.5 格左右，为避免准心看向脚下时命中不到，适当放宽一点
        double maxDist = Math.max(BetterLootingConfig.get().getActualScanRangeXZ(), 3.0);
        Vec3 from = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        Vec3 to = from.add(look.scale(maxDist));

        double bestDistSq = maxDist * maxDist;
        VisualItemEntry best = null;

        for (VisualItemEntry entry : selectionManager.getUnfilteredItems()) {
            for (ItemEntity e : entry.getSourceEntities()) {
                if (!e.isAlive()) continue;
                AABB box = e.getBoundingBox();

                // 眼睛已在碰撞箱内则视为对准；否则使用射线与碰撞箱精确求交
                Optional<Vec3> hit = box.contains(from) ? Optional.of(from) : box.clip(from, to);
                if (hit.isEmpty()) continue;

                Vec3 hitPoint = hit.get();
                double distSq = from.distanceToSqr(hitPoint);
                if (distSq > bestDistSq) continue;
                // 不可隔墙拾取：眼睛到命中点之间若被方块阻挡则忽略
                if (isBlockedByBlock(mc, from, hitPoint)) continue;

                bestDistSq = distSq;
                best = entry;
                break;
            }
        }
        return best;
    }

    /**
     * 检测眼睛到准心命中点之间是否存在阻挡方块（命中点自身的方块不算）。
     */
    private boolean isBlockedByBlock(Minecraft mc, Vec3 from, Vec3 hitPoint) {
        ClipContext ctx = new ClipContext(
                from, hitPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        );
        HitResult result = mc.level.clip(ctx);
        if (result.getType() == HitResult.Type.BLOCK) {
            // 方块命中点到眼睛的距离小于物品命中点（留微小容差），说明被墙挡住
            double blockDist = from.distanceTo(result.getLocation());
            double hitDist = from.distanceTo(hitPoint);
            return blockDist < hitDist - 0.05;
        }
        return false;
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
