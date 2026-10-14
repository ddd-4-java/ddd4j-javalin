> 2026-10-14 更新：以下内容保留为旧实现记录。当前执行契约、脚本和配置以 `docs/weekly-dependencies.md`、`.github/workflows/weekly-dependencies.yml`、`.github/dependency-policy.json` 与 `scripts/dependencies/check_dependencies.py` 为准；每个分支均保留 CI，读取目标分支策略。下文已退役的 Renovate/旧脚本路径不再作为运行指引。

# 每周依赖检查交付状态

## 2026-10-14 本地交付

规格：`../specs/2026-10-14-weekly-dependencies-design.md`。

当前分支 `feature/7.2.x`，仅修改 `.gitignore`，新增 workflow、JSON 策略、Python 执行器/测试和文档。没有修改 Java/POM，没有创建或切换分支，没有提交/推送。

已实现每周一北京时间 11:17 的 GitHub 全远端分支分页发现和 SHA 固定检出；按分支 Java/Maven 配置运行；全局/分支固定属性和排除配置；未限制的本仓库直接管理依赖升级最新正式版；当前/候选 OWASP aggregate 扫描；CVSS >= 7 或扫描错误失败；候选版本真实单测；始终上传状态、日志、补丁和报告。更新器更改保护项或将保护属性引用改成其他版本时失败。

验证：

- `python3 -m unittest discover -s scripts -p 'test_*.py'`：12 项通过（7 项新策略测试 + 5 项原扫描器测试）。新增行为先见到失败，再实现；保护属性引用绕过也有红/绿回归。
- `actionlint .github/workflows/weekly-dependencies.yml`：通过。
- `python3 scripts/check_no_records.py`：通过。
- `git diff --check`：通过。
- 隔离 Maven 3 smoke：真实执行 Versions Plugin 2.23.0 的 update-properties 和 use-latest-versions，Javalin 保持 6.7.0，commons-lang3 3.10 -> 3.21.0，两个 goal BUILD SUCCESS；保护检查版本也执行通过。日志 `/tmp/javalin-dependency-smoke.log`、`/tmp/javalin-dependency-smoke-current.log`。此 smoke 不代表 Maven 4 全分支兼容验收。
- 当前项目完整 Maven 单测：`./mvnw -U -Denforcer.skip=true -B test -DskipTests=false -Dsurefire.skip=false`，52 模块 reactor 在 MQ core 停止，BUILD FAILURE。`MqContextPropagatorTest.shouldDiscardExpiredSnapshotOnRestore`、`shouldHonorBaseContextOverridesForTtlAndCapacity`、`shouldPurgeExpiredSnapshots`、`shouldRestoreThreadContextSnapshotAcrossPublishAndConsume` 四项失败。Java/POM 未改动；属于当前基线已有失败，本次没有绕过或修复。日志 `/tmp/ddd4j-javalin-weekly-baseline-test.log`。

## 未完成验收与启用条件

- 未提交推送、未部署默认分支；定时任务尚未生效。
- GitHub 真实所有分支的 Maven 3/4 更新、单测、OWASP/NVD 数据下载扫描未运行；不能据本地测试声称所有分支通过或没有漏洞。
- 配置/核实 GitHub `MAVEN_SETTINGS_XML`；建议 `NVD_API_KEY`。进入默认分支后首次手动运行检查分支报告。
- 外部父 POM/BOM 内部的第三方版本、传递依赖仍接受漏洞检查，但由上游依赖治理修复；本 CI 不宣称强制更新所有传递版本。
- 固定/排除更新项没有漏洞豁免；最新版本不能保证绝对无漏洞。
