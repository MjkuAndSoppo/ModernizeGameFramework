package com.modernizegameframework.looting.client.core.pipeline;

import com.modernizegameframework.looting.network.NetworkHandler;
import com.modernizegameframework.looting.network.C2S.PacketBatchPickup;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 动作分发器，专门负责将客户端的拾取操作封装为网络包发送给服务端。
 * 将网络通信逻辑与核心业务逻辑解耦。
 */
public class ActionDispatcher {

    /**
     * 发送单体拾取请求，仅拾取准心实际命中的那一个掉落物实体。
     * 不再拾取整个同类分组，确保 F 键只捡起准心对准的掉落物。
     * @param entity 被准心命中的具体掉落物实体
     */
    public static void sendSinglePickup(ItemEntity entity) {
        if (entity == null || !entity.isAlive()) return;
        NetworkHandler.sendToServer(new PacketBatchPickup(List.of(entity.getId()), false, true));
    }

    /**
     * 发送批量拾取请求，拾取传入列表中的所有实体。
     * @param entities  需要拾取的掉落物实体列表
     * @param isAuto    是否是由自动模式触发的拾取
     */
    public static void sendBatchPickup(List<ItemEntity> entities, boolean isAuto) {
        List<Integer> ids = new ArrayList<>();
        entities.forEach(e -> { if(e.isAlive()) ids.add(e.getId()); });
        if (!ids.isEmpty()) {
            NetworkHandler.sendToServer(new PacketBatchPickup(ids, isAuto, false));
        }
    }

    /**
     * 发送单行拾取请求，拾取当前选中行对应同类物品的所有实体（不限数量）。
     * @param selection 选区管理器实例
     */
    public static void sendRowPickup(SelectionManager selection) {
        List<VisualItemEntry> items = selection.getNearbyItems();
        int index = selection.getSelectedIndex();

        if (index >= 0 && index < items.size()) {
            VisualItemEntry entry = items.get(index);
            List<Integer> ids = new ArrayList<>();
            entry.getSourceEntities().forEach(e -> { if(e.isAlive()) ids.add(e.getId()); });

            if (!ids.isEmpty()) {
                NetworkHandler.sendToServer(new PacketBatchPickup(ids, false, false));
            }
        }
    }

    /**
     * 处理自动拾取触发时的发包，并重置自动拾取冷却。
     * @param selection     选区管理器实例
     * @param pickupHandler 拾取处理器实例
     */
    public static void handleAutoPickup(SelectionManager selection, PickupHandler pickupHandler) {
        List<ItemEntity> targets = new ArrayList<>();
        // 自动拾取使用全量未过滤数据，避免 StabilityFilter 导致拾取延迟
        selection.getUnfilteredItems().forEach(e ->
                e.getSourceEntities().forEach(en -> { if(en.isAlive()) targets.add(en); })
        );

        if (!targets.isEmpty()) {
            sendBatchPickup(targets, true);
            // 通知 PickupHandler 已经触发了自动拾取（通常用于重新计算冷却）
            pickupHandler.onAutoPickupTriggered();
        }
    }
}