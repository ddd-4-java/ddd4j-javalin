# ddd4j-javalin 生产就绪改进设计

- **日期**：2026-08-16
- **状态**：已实施（待推送验证 CI）
- **目标**：将 ddd4j-javalin 从 internal beta（功能完成、单测全绿、容器级 IT 通过）推进到 production ready（CI/CD 自动化、质量门禁、发布工程就绪）。

## 1. 目标与范围

### 目标

本次实施三项工程改进，消除 production readiness 评估中的三个硬缺口：

1. **CI/CD workflows**：GitHub Actions 自动化构建、测试、质量检查。
2. **Quality profile**：Jacoco 覆盖率 + OWASP 依赖漏洞扫描 + SpotBugs 静态分析，统一由 Maven profile 激活。
3. **Release profile + 文档**：GPG 签名发布 profile 配置 + 发布工程演练手册（`RELEASE.md`）。

### 范围

| 项目 | 包含 | 不包含 |
|------|------|--------|
| CI workflows | build + test + quality job；PR 触发 + main 分支触发 | CD 自动发布（需阻塞项先解决） |
| Quality profile | jacoco / owasp / spotbugs 插件配置，默认 skip | 门禁阈值强制（先出数后门禁） |
| Release profile | GPG 签名 + source + javadoc + deploy 配置 | 实际发布到 Maven Central |
| 文档 | `RELEASE.md` 演练手册 + superpowers spec/plan/status | 核心仓库分叉决策文档 |

### 非目标

以下事项明确排除在本次范围之外：

- **核心仓库分叉决策**：`ddd4j` 仓库 `feature/1.0.x` 远端分叉（10 个 MQ 重构提交走 javax 方向 vs 本地 `38b0e6d6` 修复走 jakarta）的裁决——由用户决定。
- **mica 上游缺陷**：`mica-mqtt 2.6.6` 在 mac arm64 上 AIO publish 静默丢失——等待上游修复。
- **extension 空壳补全**：akka / excel / jackson / monitor / pf4j / cola / dubbo 7 个空壳模块——按需后续补。
- **`JavalinTestFixture` 完整 Ddd4jGuiceModule 集成**：当前使用 `MinimalSpiModule` 绕开——需要 core SPI 稳定后再重构。

## 2. 设计

### 2.1 CI/CD Workflows

```
.github/workflows/
├── ci.yml          # PR + push 触发：build → test → quality
└── (cd.yml)        # 未来：tag 触发自动发布（本次不实施）
```

**ci.yml 设计**：

```
┌───────────┐     ┌───────────┐     ┌───────────┐
│   Build   │────>│    Test   │────>│  Quality  │
│ mvn compile│    │ mvn verify│    │ jacoco +  │
│            │    │ (unit)    │    │ spotbugs  │
└───────────┘     └───────────┘     └───────────┘
```

- **Build job**：`mvn compile`，验证编译无误。
- **Test job**：`mvn verify`（仅单元测试，不触发容器 IT），56 模块全绿。
- **Quality job**：激活 `quality` profile，生成 Jacoco 报告 + SpotBugs 分析。

**防 OOM 策略**：分 job 而非单 job 串行，每个 job 独立 JVM 内存空间。Maven 使用 `-Xmx1g` 限制堆大小。

### 2.2 Quality Profile

```xml
<profile>
  <id>quality</id>
  <build>
    <plugins>
      <!-- Jacoco: 覆盖率报告 -->
      <plugin>jacoco-maven-plugin</plugin>
      <!-- OWASP: 依赖漏洞扫描 -->
      <plugin>dependency-check-maven</plugin>
      <!-- SpotBugs: 静态分析 -->
      <plugin>spotbugs-maven-plugin</plugin>
    </plugins>
  </build>
</profile>
```

**策略**：先出数后门禁。本次只配置插件、生成报告，不设阈值阻断构建。收集一轮基线数据后，再在后续迭代中设定覆盖率 / 漏洞等级门槛。

**激活方式**：

```bash
./mvnw verify -Pquality
```

### 2.3 Release Profile

根 `pom.xml` 已有 `release` profile 骨架（含 GPG、source、javadoc、deploy、nexus-staging 插件声明）。本次补充完整配置：

- GPG 签名默认 `skip=true`，显式传 `-Dgpg.skip=false` 才执行签名
- source + javadoc 附件
- nexus-staging 自动 close + release
- `altStagingDirectory` 支持本地 staging 验证

## 3. 关键决策与理由

| 决策 | 理由 |
|------|------|
| Quality 先出数后门禁 | 贸然设阈值会导致 CI 红灯但无基线数据可参考，先收集覆盖率/漏洞基线再设合理门槛 |
| GPG 默认 skip | 开发者本地构建不应被 GPG 签名阻断；仅在 `-Dgpg.skip=false` 显式要求时才签名 |
| CI 分 job 防 OOM | 56 模块 reactor 编译 + 测试内存消耗大，分 job 确保每个 job 独立 JVM 堆空间 |
| 不实施 CD 自动发布 | 核心依赖 SNAPSHOT 未收敛、手工 m2 jar 未消除——自动发布会将不可复现的构件推到 Central |
| Quality profile 独立于 release | quality 可在任何分支/PR 激活，不绑定发布流程；release 只在发布时激活 |

## 4. 风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| CI OOM | workflow 失败 | 分 job + Maven `-Xmx1g`；如仍 OOM 升级 runner |
| OWASP 数据库首次下载慢 | CI 首次运行超时 | 使用 `dependency-check` 的 `--data` 缓存目录 + GitHub Actions cache |
| SpotBugs 误报过多 | CI 红灯噪音大 | 首次运行收集报告，后续逐步修复高优先级项 |
| GPG 密钥泄露 | 安全风险 | 私钥仅存 GitHub Secrets，不进代码库；CI 中 Base64 解码使用 |
| 核心依赖 SNAPSHOT 阻塞正式版 | 无法发布 | 作为已知阻塞项记录在 `RELEASE.md`，不阻塞工程改进本身 |

## 5. 验收标准

| 验收项 | 验证方式 | 状态 |
|--------|---------|------|
| `quality` profile 可激活 | `./mvnw help:all-profiles \| grep quality` 存在 | 已实施 |
| `release` profile 可激活 | `./mvnw help:all-profiles \| grep release` 存在 | 已实施（骨架已存在） |
| CI workflow 文件存在 | `.github/workflows/ci.yml` 存在且语法正确 | 已实施（待推送验证） |
| `RELEASE.md` 存在且内容完整 | 文件存在，含前置条件/版本策略/演练命令/阻塞项 | 已实施 |
| CI 真实通过 | 推送后 GitHub Actions 绿灯 | 待推送后验证 |
| Jacoco 报告可生成 | `mvn verify -Pquality` 后 `target/site/jacoco/` 存在 | ⚠️ 骨架就位、agent 注入点待修（详见 report 遗留 #6） |

## 6. 关联

- 前序：[能力矩阵对齐设计](2026-07-29-javalin-capability-matrix-design.md)
- 前序：[容器级 IT 真实 round-trip 修复设计](2026-08-04-container-it-roundspec-design.md)
- 产出：[实施计划](../plans/2026-08-16-production-readiness.md)
- 状态：[状态报告](../reports/2026-08-05-status.md)
