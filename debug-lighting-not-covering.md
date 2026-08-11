# Debug Session: lighting-not-covering
- **Status**: [OPEN]
- **Issue**: 藏身处私人区域未被照明系统生成的光源方块覆盖
- **Debug Server**: http://127.0.0.1:7777/event
- **Log File**: .dbg/trae-debug-log-lighting-not-covering.ndjson

## Reproduction Steps
1. 进入藏身处维度
2. 在控制箱解锁并放置「照明」工作方块
3. 右键照明方块选择等级
4. 用拉杆/红石给照明方块提供红石信号
5. 确保供电站正在发电
6. 观察私人区域是否被光源覆盖

## Hypotheses & Verification
| ID | Hypothesis | Likelihood | Effort | Evidence |
|----|------------|------------|--------|----------|
| A | LightingSpreadManager 计算出的目标位置列表为空 | Medium | Low | 日志中 positions 数量 |
| B | 电力或红石检测未通过，desiredLevel 始终为 0 | High | Low | 日志中 hasPower/hasRedstone/desiredLevel |
| C | placeLight 被调用但 setBlock 失败（位置未加载或方块不可替换） | Medium | Low | 日志中 placementResult |
| D | 原版 light 方块放置后立即被移除或替换 | Low | Medium | 日志中 light block 存在性检查 |
| E | 照明方块位置（lightingPos）未正确记录 | Medium | Low | 日志中 lightingPos 值 |

## Log Evidence
- 日志第 1 条：`positions:512, maxRing:96` → 目标位置列表非空，但数量远小于私人区域总体积。
- 日志第 3 条及后续：`hasPower:true, hasRedstone:false, desiredLevel:0` → 红石信号断开后照明正确关闭。
- 日志第 39 条：`hasPower:true, hasRedstone:true, desiredLevel:1` → 红石信号接通后 desiredLevel 立即变为 1，电力/红石检测正常。
- 日志第 40 条：`placeLight not replaceable, pos=... lighting` → 照明方块自身位置被加入目标列表，但该位置已被工作方块占据，无法放置 light 方块。
- 多条 `tick progress` 显示 `currentRing` 正常递增，扩散框架本身工作正常。

## Verification Conclusion
| ID | Hypothesis | Status | Evidence Summary |
|----|------------|--------|------------------|
| A | 目标位置列表为空 | ❌ Rejected | 日志显示 positions=512，列表非空 |
| B | 电力或红石检测未通过 | ❌ Rejected | hasPower 稳定为 true，红石信号变化与 desiredLevel 同步 |
| C | placeLight 被调用但 setBlock 失败 | ⚠️ Confirmed (partial) | 仅照明方块自身位置不可替换，其他位置无失败日志 |
| D | 原版 light 方块放置后被立即移除 | ⏳ Inconclusive | 日志未观察到放置成功后的移除事件 |
| E | lightingPos 未正确记录 | ❌ Rejected | 日志中 lightingPos 始终为同一有效坐标 |

**根因确认**：`GRID_SPACING = 8` 导致私人区域 64×64×64 范围内仅生成 512 个光源点，网格间距过大是“只有部分区域有光”的直接原因。照明方块自身位置被包含在目标列表中，属于次要问题。

**修复方案**：
1. 将 `GRID_SPACING` 从 8 改为 1，实现私人区域满格填充。
2. 在 `computeGridPositions()` 中跳过中心点（照明工作方块自身位置）。
3. 对位置列表按环分组并缓存，避免红石频繁切换时重复计算 26 万+ 坐标。
