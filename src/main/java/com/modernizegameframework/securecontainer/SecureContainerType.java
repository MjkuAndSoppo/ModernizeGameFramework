package com.modernizegameframework.securecontainer;

/**
 * 安全箱容器类型枚举
 * 定义每种容器的名称、行列数、槽位总数和显示颜色
 */
public enum SecureContainerType {

    /** 黄色 2×2 = 4 格 */
    ALPHA("alpha", 2, 2, 0xFFD700),
    /** 橙色 3×2 = 6 格 */
    BETA("beta", 3, 2, 0xFF8C00),
    /** 黑色 3×3 = 9 格 */
    GAMMA("gamma", 3, 3, 0x2A2A2A),
    /** 绿色 3×4 = 12 格 */
    KAPPA("kappa", 3, 4, 0x00CC00),
    /** 蓝色 5×2 = 10 格 */
    THETA("theta", 5, 2, 0x3366FF);

    private final String name;
    private final int cols;
    private final int rows;
    private final int color;

    SecureContainerType(String name, int cols, int rows, int color) {
        this.name = name;
        this.cols = cols;
        this.rows = rows;
        this.color = color;
    }

    /** 容器名称（用于翻译键和注册名） */
    public String getName() {
        return name;
    }

    /** 列数 */
    public int getCols() {
        return cols;
    }

    /** 行数 */
    public int getRows() {
        return rows;
    }

    /** 总槽位数 */
    public int getSlotCount() {
        return cols * rows;
    }

    /** 显示颜色（RGB int） */
    public int getColor() {
        return color;
    }
}