# ddd4j-javalin 生产就绪改进实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans
> to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实施三项生产就绪改进（CI workflows、quality profile、release profile + 文档），消除零 CI/CD、零质量门禁、零发布工程三个硬缺口。

**Architecture:** 在 `.github/workflows/` 新增 CI workflow；在根 `pom.xml` 新增 `quality` profile（jacoco/owasp/spotbugs）；补充 `release` profile 完整配置；新增 `RELEASE.md` 发布工程手册。

**Tech Stack:** GitHub Actions、Maven profiles、jacoco-maven-plugin、dependency-check-maven、spotbugs-maven-plugin、maven-gpg-plugin

**Related Design Doc:** `docs/superpowers/specs/2026-08-16-production-readiness-design.md`

---

## 全局约定

- **Java 版本**：Java 17（`maven.compiler.source/target`）。
- **分支**：`feature/6.3.x`。
- **提交约定**：英文 conventional commits（feat/fix/docs/test/refactor/chore）。
- **不碰 pom.xml 以外的构建文件**：本次不修改 `.github/` 中其他 workflow（另一 agent 并行修改）。
- **不碰 `pom.xml`**：本次任务限定为文档文件创建/修改（另一 agent 负责 profile 配置）。

---

## 实施阶段总览

| Stage | 目标 | 状态 | Commit |
|-------|------|------|--------|
| 1 | `RELEASE.md` 发布工程演练手册 | [x] | — |
| 2 | `docs/superpowers/specs/2026-08-16-production-readiness-design.md` | [x] | — |
| 3 | `docs/superpowers/plans/2026-08-16-production-readiness.md`（本文件） | [x] | — |
| 4 | 更新 `docs/superpowers/reports/2026-08-05-status.md` | [x] | — |
| 5 | 更新 `docs/superpowers/README.md` 索引 | [x] | — |
| 6 | 更新根 `README.md` 生产就绪状态 | [x] | — |
| 7 | 链接自检 | [x] | — |

---

## Task 1: 创建 RELEASE.md

**Files:**
- Create: `RELEASE.md`

**目标:** 发布工程演练手册，覆盖前置条件、版本策略、演练命令、正式发布 checklist、首个正式版阻塞项。

- [x] **Step 1.1:** 编写前置条件章节（JDK17、GPG 密钥生成与分发、Maven settings 配置）。
- [x] **Step 1.2:** 编写版本策略章节（`${revision}` + flatten；SNAPSHOT → 正式版步骤）。
- [x] **Step 1.3:** 编写演练命令章节（release profile 的 `-Dgpg.skip=false` 用法、本地 staging 验证）。
- [x] **Step 1.4:** 编写正式发布 checklist。
- [x] **Step 1.5:** 编写首个正式版阻塞项清单（引用核心依赖 SNAPSHOT 收敛、手工 m2 jar 消除）。

## Task 2: 创建生产就绪设计文档

**Files:**
- Create: `docs/superpowers/specs/2026-08-16-production-readiness-design.md`

**目标:** 按既有 spec 模板编写，包含日期/状态/目标 + 章节（目标与范围、设计、关键决策、风险、验收标准）。

- [x] **Step 2.1:** 编写头部（日期、状态"已实施（待推送验证 CI）"、目标）。
- [x] **Step 2.2:** 编写目标与范围（本次三项 + 非目标四项）。
- [x] **Step 2.3:** 编写设计章节（CI workflow 架构、quality profile、release profile）。
- [x] **Step 2.4:** 编写关键决策与理由（5 项决策）。
- [x] **Step 2.5:** 编写风险与对策（5 项风险）。
- [x] **Step 2.6:** 编写验收标准（6 项，含 CI 真实通过待推送验证说明）。

## Task 3: 创建生产就绪实施计划

**Files:**
- Create: `docs/superpowers/plans/2026-08-16-production-readiness.md`

**目标:** 按 plan 模板，所有 checkbox 标记为 [x]，Files 清单完整，验收记录表引用三个并行产出。

- [x] **Step 3.1:** 编写计划头部（Goal、Architecture、Tech Stack、Related Design Doc）。
- [x] **Step 3.2:** 编写实施阶段总览表格（7 个 Stage 全 [x]）。
- [x] **Step 3.3:** 编写 Task 1-7，每个 Task 含 Files 清单和 Step checkbox（全 [x]）。
- [x] **Step 3.4:** 编写验收记录表。

## Task 4: 更新状态报告

**Files:**
- Modify: `docs/superpowers/reports/2026-08-05-status.md`

**目标:** 追加「历史状态」表新行（2026-08-16 三项生产就绪改进），在「已知遗留」补核心分叉决策待裁决说明。

- [x] **Step 4.1:** 在「历史状态」表末尾追加 2026-08-16 行。
- [x] **Step 4.2:** 在「已知遗留」追加 #5（核心分叉决策 javax/jakarta 待用户裁决）。
- [x] **Step 4.3:** 更新「后续计划」（无主动计划，保持原样或微调措辞）。

## Task 5: 更新 superpowers README 索引

**Files:**
- Modify: `docs/superpowers/README.md`

**目标:** 索引表追加本次 spec 和 plan 两行。

- [x] **Step 5.1:** 追加 spec 行：`| spec | 2026-08-16-production-readiness-design.md | 生产就绪改进 |`。
- [x] **Step 5.2:** 追加 plan 行：`| plan | 2026-08-16-production-readiness.md | 生产就绪实施计划 |`。

## Task 6: 更新根 README.md

**Files:**
- Modify: `README.md`

**目标:** 在「历史与版本」章节后加「生产就绪状态」小节，链接 `RELEASE.md` 与 superpowers spec。

- [x] **Step 6.1:** 在「历史与版本」与「详细文档」之间插入「生产就绪状态」章节。
- [x] **Step 6.2:** 内容：当前 internal beta、三项改进（CI/quality/release）、剩余阻塞（核心依赖收敛）。
- [x] **Step 6.3:** 链接到 `RELEASE.md` 和 `docs/superpowers/specs/2026-08-16-production-readiness-design.md`。

## Task 7: 链接自检

**Files:** 无新建/修改文件（验证步骤）。

**目标:** 确认所有新建/修改文件中的 markdown 相对链接指向实际存在的文件。

- [x] **Step 7.1:** 检查 `RELEASE.md` 中所有相对链接。
- [x] **Step 7.2:** 检查 `docs/superpowers/specs/2026-08-16-production-readiness-design.md` 中所有相对链接。
- [x] **Step 7.3:** 检查 `docs/superpowers/plans/2026-08-16-production-readiness.md` 中所有相对链接。
- [x] **Step 7.4:** 检查 `docs/superpowers/README.md` 索引表链接。
- [x] **Step 7.5:** 检查根 `README.md` 新增章节链接。
- [x] **Step 7.6:** 确认无 Spring 字样引入。

---

## 验收记录

| 产出 | 文件 | 验收方式 |
|------|------|---------|
| Release 文档 | `RELEASE.md` | 文件存在，内容覆盖前置条件/版本策略/演练/阻塞项 |
| Spec | `docs/superpowers/specs/2026-08-16-production-readiness-design.md` | 文件存在，按模板含 5 个标准章节 |
| Plan | `docs/superpowers/plans/2026-08-16-production-readiness.md` | 文件存在，checkbox 全 [x] |
| Status 更新 | `docs/superpowers/reports/2026-08-05-status.md` | 历史状态表新增行，已知遗留新增 #5 |
| Index 更新 | `docs/superpowers/README.md` | 索引表新增 2 行 |
| README 更新 | `README.md` | 新增「生产就绪状态」章节 |
| 链接自检 | 全部文件 | 无断链，无 Spring 字样 |
