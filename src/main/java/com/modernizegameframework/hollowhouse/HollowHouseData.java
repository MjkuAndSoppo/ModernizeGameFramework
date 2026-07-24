package com.modernizegameframework.hollowhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

/**
 * 藏身处玩家数据接口
 * 存储每位玩家藏身处相关的状态与权限信息
 */
public interface HollowHouseData extends INBTSerializable<CompoundTag> {

    /**
     * 获取玩家藏身处所在的区块坐标 X
     */
    int getChunkX();

    /**
     * 获取玩家藏身处所在的区块坐标 Z
     */
    int getChunkZ();

    /**
     * 设置藏身处所在的区块坐标
     */
    void setChunkPos(int chunkX, int chunkZ);

    /**
     * 判断该玩家是否已创建过藏身处
     */
    boolean isCreated();

    /**
     * 标记藏身处已创建
     */
    void markCreated();

    /**
     * 获取玩家进入藏身处前的主世界位置 X
     */
    double getReturnX();

    /**
     * 获取玩家进入藏身处前的主世界位置 Y
     */
    double getReturnY();

    /**
     * 获取玩家进入藏身处前的主世界位置 Z
     */
    double getReturnZ();

    /**
     * 获取玩家进入藏身处前所在维度的资源键字符串
     */
    String getReturnDimension();

    /**
     * 设置返回位置
     */
    void setReturnPosition(double x, double y, double z, String dimension);

    /**
     * 判断玩家当前是否在藏身处内
     */
    boolean isInsideHollowHouse();

    /**
     * 设置玩家是否在藏身处内
     */
    void setInsideHollowHouse(boolean inside);

    /**
     * 获取已邀请玩家的 UUID 集合
     */
    Set<UUID> getInvitedPlayers();

    /**
     * 邀请指定玩家
     */
    void invitePlayer(UUID playerId);

    /**
     * 取消邀请指定玩家
     */
    void revokeInvite(UUID playerId);

    /**
     * 清除所有邀请
     */
    void clearInvites();

    /**
     * 判断指定玩家是否被邀请
     */
    boolean isInvited(UUID playerId);

    /**
     * 获取控制箱是否已发放
     */
    boolean isControlBoxGiven();

    /**
     * 标记控制箱已发放
     */
    void markControlBoxGiven();

    /**
     * 判断初始平台是否已生成过
     */
    boolean isPlatformGenerated();

    /**
     * 标记初始平台已生成
     */
    void markPlatformGenerated();

    /**
     * 获取藏身处内的入口方块位置
     */
    @Nullable
    BlockPos getPortalPos();

    /**
     * 设置藏身处内的入口方块位置
     */
    void setPortalPos(@Nullable BlockPos pos);

    /**
     * 获取当前藏身处房主的 UUID
     * 被邀请玩家进入房主藏身处时，此字段记录房主身份
     */
    @Nullable
    UUID getOwnerId();

    /**
     * 设置当前藏身处房主的 UUID
     */
    void setOwnerId(@Nullable UUID ownerId);

    /**
     * 清除当前藏身处房主记录
     */
    default void clearOwnerId() {
        setOwnerId(null);
    }
}
