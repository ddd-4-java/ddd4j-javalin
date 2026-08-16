# ddd4j-javalin superpowers 规范

本目录是 ddd4j-javalin 的**规划与历史单一真相源**（对齐 liteflow 的 `docs/superpowers/` 约定）。所有新需求先写设计，再写计划，实施后回填真实状态。

## 目录约定

```
docs/superpowers/
├── README.md                    # 本文件：规范入口
├── specs/                       # 设计文档（为什么做、怎么做）
│   └── YYYY-MM-DD-<topic>-design.md
├── plans/                       # 实施计划（逐步 checkbox，可被 agent 执行）
│   └── YYYY-MM-DD-<topic>.md
└── reports/                     # 状态报告（真实执行结果，含失败与遗留）
    └── YYYY-MM-DD-status.md
```

## 命名规范

- 日期前缀 `YYYY-MM-DD` 为**设计定稿日**（不是实施日）。
- `<topic>` 用英文小写短横线分隔（如 `javalin-capability-matrix`、`testcontainers-foundation`、`container-it-roundspec`）。
- spec 文件一律带 `-design` 后缀；plan 与 report 不带。
- 同一 topic 的 plan 通过 `Related Design Doc` 头部字段链回 spec。

## 文档模板

### spec（`*-design.md`）

```markdown
# <标题>设计

- **日期**：YYYY-MM-DD
- **状态**：待实施 | 已实施（commit `<sha>`）| 已废弃
- **目标**：一段话说清要解决什么。

## 1. 目标与范围（含非目标）
## 2. 设计 / 总体架构（图表优先）
## 3. 关键决策与理由
## 4. 风险与对策
## 5. 验收标准（可执行命令 + 断言）
```

### plan（`plans/*.md`）

```markdown
# <标题>实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans
> to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 一句话目标。
**Architecture:** 模块/依赖变化概述。
**Tech Stack:** 语言版本、构建工具、关键依赖。
**Related Design Doc:** `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md`

## 全局约定（Java 版本 / groupId / 提交约定 / 测试惯例 / 包命名）

## 实施阶段总览（Stage | 目标 | 状态 | Commit 表格）

## Task N: <任务名>
**Files:** Modify/Create 清单
**目标:** 该任务完成后的可验证状态。
- [ ] **Step N.1:** 具体步骤（含失败测试优先的 TDD 顺序）
```

### report（`reports/*-status.md`）

```markdown
# ddd4j-javalin 状态报告

## 历史状态（日期 | 状态 | 备注 表格，追加不修改）

## 已知遗留（编号列出，注明等待条件）

## 后续计划（无主动计划时明确写"无"）
```

## 流程

1. **新需求** → 写 `specs/YYYY-MM-DD-<topic>-design.md`（状态：待实施）。
2. **开工** → 写 `plans/YYYY-MM-DD-<topic>.md`，逐 Task checkbox 跟踪。
3. **完成** → spec 状态改「已实施（commit `<sha>`）」；plan 底部追加「验收记录」。
4. **状态变化**（里程碑 / 容器级实测 / 遗留发现）→ 追加到 `reports/`（历史追加不修改）。
5. **旧计划核对** → 以已实现代码为准核对 plan checkbox 与状态（如本仓库 2026-08-12 的初始化核对）。

## 工程约定（写代码时必须遵守）

- **DI**：Google Guice；每模块一个 `AbstractModule`；不引入 Spring。
- **路由**：Javalin 7 编程式（`app.unsafe.routes.post(...)`）；无注解路由。
- **静态门面**：`SubjectKit.login()/getSubject()/hasPermission()`。
- **测试双轨**：单测（H2/Mockito，`mvn test`）；IT（Testcontainers，`mvn verify -Pjavalin-integration-tests`，需 Docker daemon）。
- **提交**：英文 conventional commits（feat/fix/docs/test/refactor/chore）。
- **容器镜像**：优先 arm64 native tag；版本集中在 `ddd4j-javalin-testcontainers` fixture，IT 不自建容器配置。

## 索引

| 类型 | 文件 | 主题 |
|---|---|---|
| spec | [2026-07-29-javalin-capability-matrix-design.md](specs/2026-07-29-javalin-capability-matrix-design.md) | 能力矩阵对齐（4 任务总设计） |
| spec | [2026-07-30-testcontainers-foundation-design.md](specs/2026-07-30-testcontainers-foundation-design.md) | Testcontainers Fixture 共享层 |
| spec | [2026-08-04-container-it-roundspec-design.md](specs/2026-08-04-container-it-roundspec-design.md) | 容器级 IT 真实 round-trip 修复 |
| plan | [2026-07-29-javalin-capability-matrix.md](plans/2026-07-29-javalin-capability-matrix.md) | 10 Task 实施计划 + 验收记录 |
| report | [2026-08-05-status.md](reports/2026-08-05-status.md) | 当前真实状态 |
| spec | [2026-08-16-production-readiness-design.md](specs/2026-08-16-production-readiness-design.md) | 生产就绪改进设计 |
| plan | [2026-08-16-production-readiness.md](plans/2026-08-16-production-readiness.md) | 生产就绪实施计划 |