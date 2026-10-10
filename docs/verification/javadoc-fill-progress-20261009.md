# Javadoc 补齐进度（2026-10-09）

分支：ddd4j-javalin feature/7.1.x（javalin 7.1.0 / JDK 17 / ddd4j 2.0.x 线）

## 门禁状态（已完成）

- 根 `pom.xml`：`javadoc.doclint=all`、`javadoc.failOnWarning=true`、`javadoc.failOnError=true` 三个属性 + maven-javadoc-plugin `doclint`/`failOnWarnings`/`failOnError` 引用；`additionalOptions` 含 `-Xmaxwarns 999`、`-Xmaxerrs 999`；`attach-javadocs`（goal `jar`）绑定 `verify` 生命周期。
- `ddd4j-javalin-parent/pom.xml`：pluginManagement 同步同一套门禁配置与 `verify` 绑定；默认 `<plugins>` 段激活执行（无 configuration，继承 pluginManagement），多模块均执行。
- 放宽档已删除：原 `disable-javadoc-doclint` profile（含 `-Xdoclint:none`）与 release profile 的 `<doclint>none</doclint>` 已移除，替换为注释：

  > Javadoc 零告警门禁：告警或错误一律中断构建，禁止任何形式的放宽档。

## 批次 1：auth 模块（15 文件，79 条 javadoc）

| 文件 | 插入条数 |
|---|---|
| auth-core AbstractAuthJavalinModuleTest.java（test） | 2 |
| auth-license Ddd4jLicenseJavalinModule.java | 3 |
| auth-license LicenseProperties.java | 1 |
| auth-oidc Ddd4jOidcJavalinModule.java | 4 |
| auth-oidc NimbusOidcTokenVerifier.java | 2 |
| auth-oidc OidcAuthenticationException.java | 2 |
| auth-oidc OidcProperties.java | 9 |
| auth-oidc OidcProviderUnavailableException.java | 1 |
| auth-oidc OidcSubject.java | 21 |
| auth-oidc OidcSubjectDelegate.java | 21 |
| auth-oidc OidcSubjectProvider.java | 2 |
| auth-oidc it/ProductionStartupAcceptanceIT.java（test） | 11 |
| auth-satoken Ddd4jSaTokenJavalinModule.java | 2 |
| auth-security Ddd4jSecurityJavalinModule.java | 1 |
| auth-shiro Ddd4jShiroJavalinModule.java | 1 |

套用的修复套路：

- 套路 6（`@param` 对签名）：所有方法按真实参数逐条生成 `@param`；`OidcSubject`/`OidcSubjectDelegate` 的 4 组重载（`isPermitted`/`isPermittedAny`/`isPermittedAll`/`hasRole*`）逐一独立成块。
- 套路 7（`@throws` 真实性）：`NimbusOidcTokenVerifier#verify` 补 `@throws OidcAuthenticationException`、`@throws OidcProviderUnavailableException`（两者均真实存在，extends RuntimeException）。
- 类型级：`ProductionListener` 补用途说明 + `@author PartMe.AI` + `@since 7.1.x`。
- 注解上方插入：`@Override`、`@SuppressWarnings`、`@MQEventListener` 均单行，javadoc 插入注解之上。

## 批次 2–4 及全树补齐复盘（git diff 统计）

> 后续批次的逐批明细未及落盘，以下为工作区 `git diff --numstat` 复盘统计（未提交状态）。

- 合计：177 个文件修改，+2601 / -105 行；新增 javadoc 块首行 `/**` 572 处；新增 `@param`/`@return`/`@throws`/`@since`/`@author` 标签行 848 行。
- `@since` 全树 48 处，全部为 `7.1.x`（无其他版本残留）；`@author <a href="https://github.com/partme-ai">PartMe.AI</a>` 93 处。

| 分组 | 文件数 | 新增行 |
|---|---|---|
| auth（6 子模块） | 24 | +553 |
| core | 8 | +103 |
| data（6 子模块） | 25 | +340 |
| extensions（3 子模块） | 8 | +80 |
| mq（14 子模块） | 33 | +472 |
| samples（10 子模块） | 47 | +653 |
| testcontainers | 17 | +178 |
| web | 11 | +161 |
| 构建与文档（根 pom / parent pom / README / 指南） | 4 | +61 |

## 隐患修复记录（续接会话）

- 套路 1（行首 @ 抢占）：2 个文件（`AutoIncrementIdStrategy`、`UuidIdStrategy`）普通 `<pre>` 内行首 `@` 转义为 `&#64;` 并前置转义说明注释；doclint 探针复现 4 errors → 修复后 EXIT=0。`<pre>{@code}` 内 raw `@` 共 16 处实证安全，不转义。
- 红线中性化：panache 模块 3 处非中性表述改写/删除；`@since` 7 处由旧版本号修正为 `7.1.x`。
- 套路 3（死引用）：6 处修复、3 个文件——`Ddd4jMybatisJavalinModule` 4 处改为 FQCN 链接、`Ddd4jJavalinApplication` 1 处改为 FQCN 链接、`JavalinTestFixture` 1 处不存在的方法链接改为真实方法。全量死引用扫描由 88 条可疑收敛至 4 条真实缺陷；外部成员引用（继承方法、工具类、框架类）逐个经 `javap` 反汇编验证存在。
- 套路 4/5：表格 `<caption>` 与裸尖括号全树核查，0 处需修复（30 处尖括号均为 `@param <T>` 或 `{@code}` 内代码）。

## testcontainers 镜像钉版与来源注释（2026-10-09）

钉版前 → 钉版后（经 Docker Registry v2 / Hub API / quay API 逐一实查确认 tag 存在）：

| 镜像 | 钉版前 | 钉版后 | 位置 |
|---|---|---|---|
| eclipse-mosquitto | `2.0` | `2.0.22` | 2 个 MQTT IT |
| nats | `2-alpine` | `2.10.29-alpine` | NATS IT |
| mysql | `8.0` | `8.0.46` | fixture |
| postgres | `16-alpine` | `16.15-alpine` | fixture |
| mariadb | `11` | `11.4.13` | fixture |
| mongo | `7` | `7.0.43` | fixture |
| redis | `7-alpine` | `7.4.11-alpine` | fixture |
| localstack/localstack | `3.4` | `3.4.0` | fixture + SQS IT 类级 javadoc |
| quay.io/keycloak/keycloak | `26.2` | `26.2.5` | fixture |

已钉精确版本、无需变更：`wiremock/wiremock:3.5.0`、`confluentinc/cp-kafka:7.5.0`、`rabbitmq:3.13.7-management-alpine`、`apache/activemq-artemis:2.39.0`、`apache/rocketmq:5.3.2`、`apachepulsar/pulsar:3.2.0`。

- 同步更新：`FixtureContractTest` 7 条镜像断言、`README.md` 4 行、`docs/testcontainers-guide.md` 9 行、3 个 IT 类级 javadoc 中的旧镜像描述。
- 来源注释：Java 源内 17 处 `https://testcontainers.com/modules/<slug>`（13 个 fixture 字段 javadoc + 4 个 IT 行注释），`AbstractTestContainerFixture` 类级 javadoc 增加模块目录总链；RocketMQ 无专属模块页，引用目录索引页。
- 合计 21 文件 50 处编辑 + 3 处 javadoc 镜像描述修正；新引入的 javadoc 结构（`<a href>` 锚点）doclint 探针 EXIT=0。
- 历史规格文档（`docs/superpowers/specs|plans`）按约定不改动，其镜像清单保留原记录。

## 最终复扫（2026-10-09）

- `scan_final.py`：门禁口径（public/protected 成员）`MEMBER-NO-DOC` = 0，gate-relevant = 0；剩余 617 条均为 private（384）/ 包私有（231）成员，及 2 条位于包私有或私有类型内的 public 成员——doclint 默认 protected 可见性不渲染这些元素，不触发 missing 告警。
- `validate_docs.py`：启发式命中逐类核验均为脚本误报或门禁可接受模式——record 组件类级 `@param`（探针 EXIT=0）、未列入签名的 unchecked `@throws`（探针 EXIT=0）、字段初始化器 `X.class.getName()` 被误判为方法、注释文本被误判为声明。
- `check_records.py`：13 个 record，组件 `@param` 缺失 0。
- 门禁配置落点：根 `pom.xml` L75-77（属性）、L308-318（doclint / failOnWarnings / failOnError / `-Xmaxwarns 999` / `-Xmaxerrs 999`）、L458-459（放宽档删除注释）；`ddd4j-javalin-parent/pom.xml` L112-122、L167（verify 绑定）、L335-336。
- 工作区状态：177 文件脏状态保留，未 commit / 未 push，待主会话审计后统一检查点提交。
