# ddd4j-javalin 发布工程演练手册

> 本手册指导如何将 ddd4j-javalin 从 SNAPSHOT 发布为正式版到 Maven Central（及内部 Codeup 仓库）。

## 前置条件

### JDK 与构建工具

- JDK 17（`java -version` 确认）
- Maven 3.6.3+（推荐使用仓库自带 `./mvnw`）
- Docker daemon 可用（发布前需跑通全部集成测试）

### GPG 密钥

Maven Central 要求所有构件使用 GPG 签名。

**生成密钥对**（首次）：

```bash
gpg --full-generate-key
# 选择 RSA and RSA, 4096 bits, 无过期
# Real name: ddd4j
# Email: your-email@example.com
```

**导出公钥并分发**：

```bash
# 查看密钥 ID
gpg --list-keys

# 上传到公钥服务器（Maven Central 校验用）
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>

# 导出私钥供 CI 使用（Base64 编码后存入 GitHub Secrets）
gpg --export-secret-keys <KEY_ID> | base64 > gpg-key.txt
```

**本地 `~/.m2/settings.xml` 配置**（手动发布时）：

```xml
<settings>
  <servers>
    <!-- Maven Central (Sonatype OSSRH) -->
    <server>
      <id>central</id>
      <username>${CENTRAL_USERNAME}</username>
      <password>${CENTRAL_PASSWORD}</password>
    </server>
    <!-- 阿里云 Codeup 制品仓库（内部分发） -->
    <server>
      <id>codeup</id>
      <username>${CODEUP_USERNAME}</username>
      <password>${CODEUP_TOKEN}</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>ossrh</id>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>${GPG_PASSPHRASE}</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>ossrh</activeProfile>
  </activeProfiles>
</settings>
```

### 发布仓库配置

根 `pom.xml` 已通过 `distributionManagement` 配置 `central`（OSSRH）和 `codeup` 两个发布仓库。确认 `server.id` 与 `repository.id` 一致。

---

## 版本策略

### ${revision} + flatten 机制

本仓库使用 Maven CI-friendly 版本号（`${revision}`），配合 `flatten-maven-plugin` 打平 POM：

| 阶段 | `${revision}` 值 | 说明 |
|------|------------------|------|
| 开发中 | `1.0.x.20260630-SNAPSHOT` | 当前状态 |
| RC | `1.0.0-rc1` | 发布候选 |
| 正式版 | `1.0.0` | Maven Central 发布 |
| 下一迭代 | `1.0.1-SNAPSHOT` | 正式版发布后立即设置 |

### SNAPSHOT 转正式版步骤

```bash
# 1. 确认当前快照可构建
./mvnw clean verify -Pjavalin-integration-tests

# 2. 修改 pom.xml 中 <revision> 为正式版号
#    根 pom.xml: <revision>1.0.0</revision>

# 3. 构建并签名（release profile 启用 GPG）
./mvnw clean deploy -Prelease -Dgpg.skip=false

# 4. 验证 staging 仓库中的构件
#    登录 https://oss.sonatype.org → Staging Repositories
#    确认 io.ddd4j.javalin 构件完整（jar + sources + javadoc + .asc）

# 5. Close & Release staging（或用 nexus-staging-maven-plugin 自动完成）

# 6. 发布后立即设置下一开发版本
#    根 pom.xml: <revision>1.0.1-SNAPSHOT</revision>
#    git commit -am "chore: prepare for next development iteration 1.0.1-SNAPSHOT"
```

---

## 发布演练（本地 staging 验证）

在正式发布前，先在本地 staging 目录验证构件完整性：

```bash
# 构建并部署到本地 staging（不上传远程）
./mvnw clean deploy -Prelease \
  -Dgpg.skip=false \
  -DaltStagingDirectory=${project.basedir}/target/staging \
  -DskipRemoteStaging=true

# 验证 staging 内容
ls -la target/staging/io/ddd4j/javalin/

# 检查每个模块是否包含：
# - <module>.jar
# - <module>-sources.jar
# - <module>-javadoc.jar
# - <module>.jar.asc（GPG 签名）
# - <module>.pom（flatten 后的真实版本号）

# 验证 GPG 签名
gpg --verify target/staging/io/ddd4j/javalin/ddd4j-javalin-core/1.0.0/ddd4j-javalin-core-1.0.0.jar.asc
```

---

## 正式发布 Checklist

发布前逐项确认：

- [ ] **核心依赖已收敛**：`ddd4j` 父 POM `2.0.x.*` 为正式版（非 SNAPSHOT）——参见「首个正式版阻塞项」
- [ ] **`mvn clean verify` 全绿**：56 模块单元测试 + 10 个容器级 IT 全部通过
- [ ] **版本号已更新**：`<revision>` 从 SNAPSHOT 改为正式版号
- [ ] **GPG 密钥就绪**：本地或 CI 环境可签名
- [ ] **`settings.xml` 已配置**：`central` 和/或 `codeup` 仓库凭证
- [ ] **CHANGELOG 已更新**：记录自上个版本以来的变更
- [ ] **`release` profile 可激活**：`./mvnw help:all-profiles | grep release` 确认存在
- [ ] **本地 staging 验证通过**：构件完整、签名有效
- [ ] **远端仓库可连通**：`curl -I https://oss.sonatype.org` 无超时

---

## 首个正式版阻塞项

以下阻塞项必须在首个正式版发布前解决：

### 阻塞项 1：核心依赖 SNAPSHOT 收敛

**现状**：根 `pom.xml` 的父 POM `ddd4j-parent:2.0.x.20260630-SNAPSHOT` 为快照版本。Maven Central 拒绝接受依赖 SNAPSHOT 的正式版构件。

**解决路径**：
1. 在 `ddd4j` 核心仓库（`feature/2.0.x`）完成 SNAPSHOT → 正式版发布
2. 或：在本仓库 `pom.xml` 中将 `<parent>` 版本锁定为已发布的正式版号

**关联**：核心仓库 `feature/1.0.x` 远端分叉问题（10 个 MQ 重构提交走 javax 方向 vs 本地修复走 jakarta）需先裁决——这是核心依赖收敛的前提。参见 [superpowers 状态报告](docs/superpowers/reports/2026-08-05-status.md) 已知遗留 #2。

### 阻塞项 2：手工 m2 jar 消除

**现状**：核心仓库 commit `38b0e6d6` 的修复（ActiveMQ JMS 属性名 sanitize、Artemis MessageID 前缀等）仅存在于本地，通过手工安装到 `~/.m2/repository` 生效。构建不可复现。

**解决路径**：
1. 核心仓库远端分叉裁决后，将修复合并到正式分支
2. 发布 `ddd4j` 核心正式版，本仓库自动拉取

---

## CI/CD 发布（GitHub Actions）

CI/CD 流程由 `.github/workflows/` 中的 workflow 文件定义。发布触发条件：

```yaml
# 手动触发（workflow_dispatch）或 tag 推送
on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Release version (e.g. 1.0.0)'
        required: true
  push:
    tags:
      - 'v*'
```

发布 workflow 需要以下 GitHub Secrets：

| Secret | 用途 |
|--------|------|
| `GPG_PRIVATE_KEY` | GPG 私钥（Base64） |
| `GPG_PASSPHRASE` | GPG 密码 |
| `CENTRAL_USERNAME` | Sonatype OSSRH 用户名 |
| `CENTRAL_PASSWORD` | Sonatype OSSRH 密码 |
| `CODEUP_USERNAME` | 阿里云 Codeup 用户名 |
| `CODEUP_TOKEN` | 阿里云 Codeup Token |

---

## 相关文档

- [生产就绪设计文档](docs/superpowers/specs/2026-08-16-production-readiness-design.md)
- [状态报告（已知遗留）](docs/superpowers/reports/2026-08-05-status.md)
- [架构文档](docs/architecture.md)
- [集成测试指南](docs/testcontainers-guide.md)
