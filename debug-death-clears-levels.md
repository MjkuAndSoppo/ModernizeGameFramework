# Debug Session: death-clears-levels
- **Status**: [OPEN]
- **Issue**: 玩家死亡后藏身处工作方块等级被清空
- **Debug Server**: 待启动后填写
- **Log File**: .dbg/trae-debug-log-death-clears-levels.ndjson

## Reproduction Steps
1. 玩家进入藏身处并解锁若干工作方块等级（如医疗站、供电站、照明等）。
2. 使用 `/kill` 或其他方式使玩家死亡。
3. 玩家重生后检查工作方块等级，发现等级被重置为 0。

## Hypotheses & Verification
| ID | Hypothesis | Likelihood | Effort | Evidence |
|----|------------|------------|--------|----------|
| A | `PlayerEvent.Clone` 未触发或新玩家实体尚未附加能力，导致旧数据无法复制 | High | Low | Pending |
| B | `original.getCapability()` 在克隆时返回空/无效，旧数据已丢失 | Medium | Low | Pending |
| C | 克隆成功复制数据，但后续事件（如 `onPlayerRespawn`）覆盖了数据 | Medium | Low | Pending |
| D | 数据正常保存到 NBT，但重生后能力反序列化时未正确读取 `workBlockLevels` | Medium | Low | Pending |
| E | 其他代码在玩家死亡/重生时主动清空或重置了等级 | Low | Medium | Pending |

## Log Evidence
待收集

## Verification Conclusion
待填写
