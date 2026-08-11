package com.modernizegameframework.hollowhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
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
     * 获取工作方块等级映射
     * 键为工作方块 ID，值为当前等级；0 表示未解锁
     */
    Map<String, Integer> getWorkBlockLevels();

    /**
     * 获取指定工作方块的当前等级
     *
     * @param workBlockId 工作方块 ID
     * @return 当前等级，0 表示未解锁
     */
    int getWorkBlockLevel(String workBlockId);

    /**
     * 设置指定工作方块的等级
     *
     * @param workBlockId 工作方块 ID
     * @param level 目标等级，0 表示未解锁
     */
    void setWorkBlockLevel(String workBlockId, int level);

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

    /**
     * 获取仓库容器（最大容量，包含所有等级解锁的格子）
     */
    SimpleContainer getStorehouseInventory();

    /**
     * 从 NBT 标签加载仓库容器内容
     */
    void loadStorehouseInventory(CompoundTag tag);

    /**
     * 将仓库容器内容保存为 NBT 标签
     */
    CompoundTag saveStorehouseInventory();

    /**
     * 获取医疗站当前生产任务列表
     */
    List<MedicalTask> getMedicalTasks();

    /**
     * 添加医疗站生产任务
     */
    void addMedicalTask(MedicalRecipe recipe, int amount);

    /**
     * 设置医疗站生产任务列表（用于反序列化）
     */
    void setMedicalTasks(List<MedicalTask> tasks);

    /**
     * 推进医疗站生产任务进度，并返回是否有产出或任务完成
     *
     * @param player 用于同步任务列表的玩家
     */
    void tickMedicalTasks(ServerPlayer player);

    /**
     * 移除指定索引的医疗站任务
     *
     * @param index 任务索引
     */
    void removeMedicalTask(int index);

    /**
     * 获取供电站数据
     */
    PowerStationData getPowerStationData();

    /**
     * 设置供电站数据（用于反序列化）
     */
    void setPowerStationData(PowerStationData data);

    /**
     * 获取供电站方块位置
     */
    @Nullable
    BlockPos getPowerStationPos();

    /**
     * 设置供电站方块位置
     */
    void setPowerStationPos(@Nullable BlockPos pos);

    /**
     * 获取照明工作方块数据
     */
    LightingData getLightingData();

    /**
     * 设置照明工作方块数据
     */
    void setLightingData(LightingData data);

    /**
     * 获取照明方块位置
     */
    @Nullable
    BlockPos getLightingPos();

    /**
     * 设置照明方块位置
     */
    void setLightingPos(@Nullable BlockPos pos);
}
