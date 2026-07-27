package com.modernizegameframework.hollowhouse;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 藏身处工作方块管理器
 * 处理工作方块的解锁与升级逻辑
 */
public class HollowHouseWorkBlockManager {

    /**
     * 尝试解锁或升级指定工作方块
     *
     * @param player 玩家
     * @param workBlockId 工作方块 ID
     */
    public static void tryUpgradeWorkBlock(ServerPlayer player, String workBlockId) {
        HollowHouseWorkBlockType type = HollowHouseWorkBlockType.fromId(workBlockId);
        if (type == null) {
            player.sendSystemMessage(Component.literal("§c未知的工作方块"));
            return;
        }

        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            player.sendSystemMessage(Component.literal("§c无法获取藏身处数据"));
            return;
        }

        // 仅房主可解锁/升级
        if (!player.getUUID().equals(data.getOwnerId())) {
            player.sendSystemMessage(Component.literal("§c只有藏身处房主才能解锁或升级工作方块"));
            return;
        }

        int currentLevel = data.getWorkBlockLevel(workBlockId);
        if (currentLevel >= type.getMaxLevel()) {
            player.sendSystemMessage(Component.literal("§c该工作方块已达到最高等级"));
            return;
        }

        int targetLevel = currentLevel + 1;
        int cost = type.getUpgradeCost(targetLevel);

        if (player.experienceLevel < cost) {
            player.sendSystemMessage(Component.literal("§c经验等级不足，需要 §e" + cost + " §c级"));
            return;
        }

        player.giveExperienceLevels(-cost);
        data.setWorkBlockLevel(workBlockId, targetLevel);

        String action = currentLevel == 0 ? "解锁" : "升级";
        player.sendSystemMessage(Component.literal(
                "§a成功" + action + " §e" + type.getDisplayName() + " §a至 §e" + targetLevel + " §a级"));

        // 同步最新等级到客户端，刷新 UI
        HollowHouseWorkBlockNetwork.syncToClient(player, data.getWorkBlockLevels());
    }
}
