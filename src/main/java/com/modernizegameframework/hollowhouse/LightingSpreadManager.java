package com.modernizegameframework.hollowhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 照明扩散管理器
 * 以照明方块为中心，按六向 BFS 波次在私人区域中放置或移除原版 light 方块
 * 每一 tick 处理一个曼哈顿距离环，实现逐级扩散效果
 */
public class LightingSpreadManager {

    /**
     * 光源网格间隔，1 表示满格填充私人区域
     */
    private static final int GRID_SPACING = 1;

    /**
     * 私人区域半边长
     */
    private static final int AREA_HALF_SIZE = 32;

    /**
     * 当前扩散中心（照明方块位置）
     */
    @Nullable
    private BlockPos center;

    /**
     * 当前私人区域中心
     */
    @Nullable
    private BlockPos areaCenter;

    /**
     * 目标光照等级，0 表示关闭
     */
    private int desiredLevel;

    /**
     * 当前处理到第几个环
     */
    private int currentRing;

    /**
     * 最大环数
     */
    private int maxRing;

    /**
     * 当前是否处于移除模式
     */
    private boolean removing;

    /**
     * 按环索引分组的目标位置列表
     * rings.get(i) 表示曼哈顿距离为 i 的所有位置
     */
    private List<List<BlockPos>> rings = Collections.emptyList();

    /**
     * 缓存同一中心/区域对应的位置分组，避免红石频繁切换时重复计算
     */
    @Nullable
    private BlockPos cachedCenter;

    @Nullable
    private BlockPos cachedAreaCenter;

    private List<List<BlockPos>> cachedRings = Collections.emptyList();

    private int cachedMaxRing;

    /**
     * 更新目标状态，当中心点、区域中心或目标等级变化时重新计算队列
     */
    public void update(@Nullable BlockPos center, int desiredLevel, BlockPos areaCenter) {
        boolean centerChanged = this.center == null || !this.center.equals(center);
        boolean levelChanged = this.desiredLevel != desiredLevel;
        boolean areaChanged = this.areaCenter == null || !this.areaCenter.equals(areaCenter);

        if (!centerChanged && !levelChanged && !areaChanged) {
            return;
        }

        this.center = center;
        this.areaCenter = areaCenter;
        this.desiredLevel = desiredLevel;
        this.currentRing = 0;
        this.removing = desiredLevel <= 0 || center == null;

        if (center == null) {
            this.rings = Collections.emptyList();
            this.maxRing = 0;
            return;
        }

        // 仅当中心或区域变化时才重新计算坐标，红石/等级切换时复用缓存
        if (cachedCenter == null || !cachedCenter.equals(center)
                || cachedAreaCenter == null || !cachedAreaCenter.equals(areaCenter)) {
            List<BlockPos> positions = computeGridPositions(center, areaCenter);
            cachedMaxRing = computeMaxRing(positions, center);
            cachedRings = groupByRing(positions, center, cachedMaxRing);
            cachedCenter = center;
            cachedAreaCenter = areaCenter;
        }

        this.rings = cachedRings;
        this.maxRing = cachedMaxRing;
    }

    /**
     * 推进一帧扩散逻辑
     */
    public void tick(ServerLevel level) {
        if (center == null || rings.isEmpty() || currentRing > maxRing) {
            return;
        }

        List<BlockPos> ringPositions = rings.get(currentRing);
        for (BlockPos pos : ringPositions) {
            if (removing) {
                removeLight(level, pos);
            } else {
                placeLight(level, pos, desiredLevel);
            }
        }

        currentRing++;
    }

    /**
     * 判断当前是否还有未完成的扩散任务
     */
    public boolean isProcessing() {
        return center != null && currentRing <= maxRing;
    }

    /**
     * 强制立即清除所有已知光源位置
     */
    public void clearAll(ServerLevel level) {
        for (List<BlockPos> ringPositions : rings) {
            for (BlockPos pos : ringPositions) {
                removeLight(level, pos);
            }
        }
        currentRing = maxRing + 1;
    }

    /**
     * 计算以中心点为基准、位于私人区域内的网格位置
     */
    private static List<BlockPos> computeGridPositions(BlockPos center, BlockPos areaCenter) {
        List<BlockPos> positions = new ArrayList<>();
        int minX = areaCenter.getX() - AREA_HALF_SIZE;
        int maxX = areaCenter.getX() + AREA_HALF_SIZE;
        int minY = Math.max(levelMinY(), areaCenter.getY() - AREA_HALF_SIZE);
        int maxY = Math.min(levelMaxY(), areaCenter.getY() + AREA_HALF_SIZE);
        int minZ = areaCenter.getZ() - AREA_HALF_SIZE;
        int maxZ = areaCenter.getZ() + AREA_HALF_SIZE;

        // 计算各个方向需要覆盖的网格索引范围
        int iMin = floorDiv(minX - center.getX(), GRID_SPACING);
        int iMax = floorDiv(maxX - center.getX(), GRID_SPACING);
        int jMin = floorDiv(minY - center.getY(), GRID_SPACING);
        int jMax = floorDiv(maxY - center.getY(), GRID_SPACING);
        int kMin = floorDiv(minZ - center.getZ(), GRID_SPACING);
        int kMax = floorDiv(maxZ - center.getZ(), GRID_SPACING);

        for (int i = iMin; i <= iMax; i++) {
            for (int j = jMin; j <= jMax; j++) {
                for (int k = kMin; k <= kMax; k++) {
                    BlockPos pos = center.offset(i * GRID_SPACING, j * GRID_SPACING, k * GRID_SPACING);
                    // 跳过照明工作方块自身位置，避免放置失败
                    if (pos.equals(center)) {
                        continue;
                    }
                    if (isInsideArea(pos, areaCenter)) {
                        positions.add(pos);
                    }
                }
            }
        }

        return positions;
    }

    /**
     * 将位置列表按曼哈顿距离环分组
     */
    private static List<List<BlockPos>> groupByRing(List<BlockPos> positions, BlockPos center, int maxRing) {
        List<List<BlockPos>> ringLists = new ArrayList<>(maxRing + 1);
        for (int i = 0; i <= maxRing; i++) {
            ringLists.add(new ArrayList<>());
        }
        for (BlockPos pos : positions) {
            int ring = computeRing(pos, center);
            ringLists.get(ring).add(pos);
        }
        return ringLists;
    }

    private static int computeRing(BlockPos pos, BlockPos center) {
        return Math.abs(pos.getX() - center.getX())
                + Math.abs(pos.getY() - center.getY())
                + Math.abs(pos.getZ() - center.getZ());
    }

    private static int computeMaxRing(List<BlockPos> positions, BlockPos center) {
        int max = 0;
        for (BlockPos pos : positions) {
            max = Math.max(max, computeRing(pos, center));
        }
        return max;
    }

    private static boolean isInsideArea(BlockPos pos, BlockPos areaCenter) {
        return pos.getX() >= areaCenter.getX() - AREA_HALF_SIZE
                && pos.getX() <= areaCenter.getX() + AREA_HALF_SIZE
                && pos.getY() >= areaCenter.getY() - AREA_HALF_SIZE
                && pos.getY() <= areaCenter.getY() + AREA_HALF_SIZE
                && pos.getZ() >= areaCenter.getZ() - AREA_HALF_SIZE
                && pos.getZ() <= areaCenter.getZ() + AREA_HALF_SIZE;
    }

    private static int levelMinY() {
        return -64;
    }

    private static int levelMaxY() {
        return 320;
    }

    private static int floorDiv(int x, int y) {
        return Math.floorDiv(x, y);
    }

    /**
     * 根据照明等级获取对应的光照等级
     * 1 级 = 4，2 级 = 8，3 级 = 12
     */
    private static int getLightLevelForLightingLevel(int lightingLevel) {
        return switch (lightingLevel) {
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 12;
            default -> 0;
        };
    }

    /**
     * 在指定位置放置/替换为指定等级的原版 light 方块
     */
    private static void placeLight(ServerLevel level, BlockPos pos, int lightingLevel) {
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        int lightLevel = getLightLevelForLightingLevel(lightingLevel);

        if (existing.getBlock() instanceof LightBlock) {
            int currentLevel = existing.getValue(BlockStateProperties.LEVEL);
            if (currentLevel != lightLevel) {
                level.setBlock(pos, Blocks.LIGHT
                        .defaultBlockState().setValue(BlockStateProperties.LEVEL, lightLevel), Block.UPDATE_ALL);
            }
            return;
        }

        if (existing.isAir() || existing.canBeReplaced()) {
            level.setBlock(pos, Blocks.LIGHT
                    .defaultBlockState().setValue(BlockStateProperties.LEVEL, lightLevel), Block.UPDATE_ALL);
        }
    }

    /**
     * 移除指定位置的原版 light 方块
     */
    private static void removeLight(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        if (existing.getBlock() instanceof LightBlock) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

}
