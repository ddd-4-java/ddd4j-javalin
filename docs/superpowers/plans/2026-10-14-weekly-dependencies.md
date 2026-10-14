> 2026-10-14 更新：以下内容保留为旧实现记录。当前执行契约、脚本和配置以 `docs/weekly-dependencies.md`、`.github/workflows/weekly-dependencies.yml`、`.github/dependency-policy.json` 与 `scripts/dependencies/check_dependencies.py` 为准；每个分支均保留 CI，读取目标分支策略。下文已退役的 Renovate/旧脚本路径不再作为运行指引。

# 每周全分支依赖检查实施计划

Related Design Doc: ../specs/2026-10-14-weekly-dependencies-design.md

- [x] 读取当前分支、现有 workflow、Maven 配置和规格事实源。
- [x] 编写策略测试并确认执行器缺失时 6 项行为断言失败。
- [x] 实现配置、分支覆盖、固定属性、排除保护和 Maven 更新执行。
- [x] 实现分支发现、baseline/candidate 漏洞扫描、单测和报告上传。
- [x] 本地策略测试、actionlint、项目脚本检查与文档。
- [ ] GitHub 默认分支部署和真实全分支运行（未获提交推送授权）。

验收记录：见 `../reports/2026-10-14-weekly-dependencies-status.md`。完整 Maven 单测有现有四项失败，远端扫描及启用仍待验证。
