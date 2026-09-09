# ddd4j-javalin Three-Branch Convergence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Subagent execution and Git worktrees are prohibited for this change.

**Goal:** Converge the three official ddd4j-javalin branches onto their matching ddd4j/Javalin/Maven/JDK lines and prove them with executable build contracts, full unit tests, Testcontainers round-trips, and branch-correct GitHub Actions.

**Architecture:** Preserve the existing Guice + Javalin adapter design and converge already implemented candidate work instead of rebuilding it. Each branch has an explicit build contract: 6.7.x and 7.1.x use Maven 3/POM 4.0.0, while 7.2.x uses Maven 4/POM 4.1.0; all functional verification is performed against the matching deployed ddd4j line.

**Tech Stack:** Java 17/21, Maven 3/4, Javalin 6.7.0/7.1.0/7.2.3, Google Guice, JUnit 5, AssertJ, Testcontainers 1.20.6, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-07-three-branch-convergence-design.md`

## Global Constraints

- `feature/6.7.x` = `6.7.x.20260630-SNAPSHOT` + ddd4j `1.0.x.20260630-SNAPSHOT` + Javalin `6.7.0` + Maven 3/POM 4.0.0 + JDK 17.
- `feature/7.1.x` = `7.1.x.20260630-SNAPSHOT` + ddd4j `2.0.x.20260630-SNAPSHOT` + Javalin `7.1.0` + Maven 3/POM 4.0.0 + JDK 17.
- `feature/7.2.x` = `7.2.x.20260630-SNAPSHOT` + ddd4j `3.0.x.20260630-SNAPSHOT` + Javalin `7.2.3` + Maven 4/POM 4.1.0 + JDK 21.
- Maven 3 branches use `<modules>/<module>`; only the Maven 4 branch uses `<subprojects>/<subproject>`.
- Keep Testcontainers at `1.20.6`; do not combine this convergence with a Testcontainers 2.x upgrade.
- Never use, create, simulate, or depend on a Git worktree.
- Preserve the dirty `ddd4j-boot` checkout; it is read-only reference material.
- Do not push without explicit authorization. Local commits are required before switching official branches and therefore have a separate authorization gate.
- Use a fresh temporary Maven local repository for publication-consumption proof; a developer's populated `~/.m2` is not release evidence.
- Treat source/POM checks, dependency resolution, unit tests, container IT, GitHub Actions, and remote SHA equality as separate evidence levels.

---

### Task 1: Add an executable build-line contract

**Files:**
- Create: `ddd4j-javalin-testcontainers/src/test/java/io/ddd4j/javalin/testcontainers/BuildLineContractTest.java`
- Modify: `ddd4j-javalin-testcontainers/pom.xml`

**Interfaces:**
- Consumes: root `pom.xml`, `ddd4j-javalin-dependencies/pom.xml`, `.mvn/wrapper/maven-wrapper.properties`, `.github/workflows/ci.yml`, `.github/workflows/integration-it.yml`.
- Produces: a JUnit 5 contract that derives the line from root `<revision>` and rejects incompatible ddd4j, Javalin, Maven model, aggregate element, wrapper, JDK, or workflow branch combinations.

- [x] **Step 1: Write the failing test for the current 6.7.x mismatch**

  Add a table-driven test with literal expectations:

  ```java
  private static final Map<String, BuildLine> LINES = Map.of(
          "6.7.x.20260630-SNAPSHOT", new BuildLine("1.0.x.20260630-SNAPSHOT", "6.7.0", "4.0.0", "modules", "17", "3."),
          "7.1.x.20260630-SNAPSHOT", new BuildLine("2.0.x.20260630-SNAPSHOT", "7.1.0", "4.0.0", "modules", "17", "3."),
          "7.2.x.20260630-SNAPSHOT", new BuildLine("3.0.x.20260630-SNAPSHOT", "7.2.3", "4.1.0", "subprojects", "21", "4.")
  );
  ```

  Parse POMs with JDK DOM APIs, not regex. Assert the root parent and `<ddd4j.version>` equal the expected ddd4j version, the dependencies POM exposes one unambiguous runtime Javalin property, and the aggregation/model match the line.

- [x] **Step 2: Run the contract and verify RED**

  Run:

  ```bash
  mvn -B -ntp -Denforcer.skip=true \
    -pl ddd4j-javalin-testcontainers -am \
    -DskipTests=false -Dsurefire.skip=false \
    -Dtest=BuildLineContractTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected on the current formal 6.7.x branch: FAIL because the root parent/`ddd4j.version` is `2.0.x.20260730-SNAPSHOT`, not `1.0.x.20260630-SNAPSHOT`. If dependency resolution prevents the test JVM from starting, capture that as the earlier publication/configuration failure and run the test in the 1.0.x candidate after Task 2 establishes its reactor.

- [x] **Step 3: Add wrapper and workflow assertions**

  Assert observable configuration contracts:

  - Maven 3 lines' wrapper distribution contains `/apache-maven/3.`.
  - Maven 4 line's wrapper distribution contains `/apache-maven/4.`.
  - CI and IT YAML contain the current line's exact official branch and exact JDK.
  - Both workflows consume `secrets.MAVEN_SETTINGS_XML` through an environment variable.
  - Neither workflow contains job-level `continue-on-error: true`.

- [x] **Step 4: Re-run to preserve the expected RED state**

  Use the command from Step 2. Record the exact failing assertions; failures caused only by typos or inability to locate the repository root must be fixed before proceeding.

### Task 2: Converge `feature/6.7.x` with the 1.0.x retarget candidate

**Files:**
- Modify: `pom.xml`
- Modify: `ddd4j-javalin-dependencies/pom.xml`
- Modify: `ddd4j-javalin-bom/pom.xml`
- Modify: `.mvn/wrapper/maven-wrapper.properties`
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/integration-it.yml`
- Review/merge: `ddd4j-javalin-guice-bridge/**`
- Review/merge: candidate changes under `ddd4j-javalin-{auth,cache,core,data,extensions,mq,samples,testcontainers,web}/**`

**Interfaces:**
- Consumes: `opt/retarget-1.0.x`, formal `feature/6.7.x`, ddd4j `feature/1.0.x` deployed artifacts.
- Produces: official `feature/6.7.x` with the 1.0.x compatibility bridge and later formal-branch auth/Web/test fixes.

- [x] **Step 1: Create a commit and conflict ledger without changing branches**

  Run:

  ```bash
  git log --left-right --cherry-pick --oneline feature/6.7.x...opt/retarget-1.0.x
  git diff --name-status feature/6.7.x..opt/retarget-1.0.x
  git merge-tree "$(git merge-base feature/6.7.x opt/retarget-1.0.x)" feature/6.7.x opt/retarget-1.0.x
  ```

  Classify each unique commit/file as candidate-required, formal-required, obsolete because upstream 1.0.x now supplies it, or documentation-only. Save the classification in the plan's validation record; do not create another specification.

- [x] **Step 2: Stop for local commit authorization**

  The repository currently contains the approved spec and plan as untracked changes. Obtain explicit authorization before making the local documentation checkpoint or any merge/cherry-pick commit. Do not stash, reset, clean, or use a worktree to bypass this gate.

- [x] **Step 3: Establish the integrated 6.7.x branch**

  After authorization, commit the approved spec/plan, integrate the candidate history into `feature/6.7.x`, and resolve conflicts according to the ledger. Preserve:

  - root/managed ddd4j version `1.0.x.20260630-SNAPSHOT`;
  - Javalin runtime `6.7.0` with a single clearly named runtime property;
  - Maven 3 `4.0.0/<modules>` structure;
  - JDK 17 build target;
  - Guice bridge types only when absent from the newly deployed 1.0.x artifacts.

- [x] **Step 4: Run the build contract and verify GREEN**

  Run the Task 1 focused command. Expected: `BuildLineContractTest` PASS.

- [x] **Step 5: Prove resolved versions**

  Run with Maven 3 and the configured settings:

  ```bash
  mvn -U -B -ntp -Denforcer.skip=true \
    -pl ddd4j-javalin-web -am \
    dependency:tree \
    -Dincludes=io.ddd4j:*,io.javalin:javalin
  ```

  Expected: only ddd4j `1.0.x.20260630-SNAPSHOT` and Javalin `6.7.0`; no ddd4j 2.x/3.x or Javalin 7.x artifact.

- [x] **Step 6: Run full unit tests**

  ```bash
  mvn -U -B -ntp -Denforcer.skip=true test \
    -DskipTests=false -Dsurefire.skip=false
  ```

  Expected: BUILD SUCCESS with actual executed-test counts; zero failures/errors. Do not reuse the historical 142-test result as current evidence.

### Task 3: Restore `feature/7.1.x` to Maven 3 and ddd4j 2.0.x

**Files:**
- Modify: every tracked `**/pom.xml` containing Maven model/schema or aggregate elements
- Modify: `.mvn/wrapper/maven-wrapper.properties`
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/integration-it.yml`
- Modify: `README.md`
- Modify: `docs/javalin-version-matrix.md`

**Interfaces:**
- Consumes: integrated shared tests/fixes from Task 2 where source-compatible, ddd4j `2.0.x.20260630-SNAPSHOT`.
- Produces: Javalin 7.1.0 adapter built entirely with Maven 3/POM 4.0.0 semantics.

- [x] **Step 1: Apply the build contract and verify RED on 7.1.x**

  Expected failures:

  - ddd4j version is `2.0.x.20260730-SNAPSHOT`;
  - POMs use `modelVersion 4.1.0` and `<subprojects>`;
  - wrapper points at Maven 4;
  - workflows listen to `feature/7.2.x` and describe ddd4j 3.0.x.

- [x] **Step 2: Convert all POMs mechanically to Maven 3 model**

  For every production POM:

  ```xml
  <project xmlns="http://maven.apache.org/POM/4.0.0"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>
  </project>
  ```

  Replace `<subprojects>/<subproject>` with `<modules>/<module>` without changing module order or membership. Validate every POM with `xmllint --noout`.

- [x] **Step 3: Correct versions and Maven wrapper**

  Set root parent and `<ddd4j.version>` to `2.0.x.20260630-SNAPSHOT`, keep project revision `7.1.x.20260630-SNAPSHOT`, keep Javalin `7.1.0`, and select the Maven 3 wrapper version proven by the current ddd4j 2.0.x workflow/wrapper rather than inventing a version.

- [x] **Step 4: Correct both workflows**

  Make both workflows explicitly target `feature/7.1.x`, JDK 17, Maven 3, ddd4j 2.0.x, and raw-XML `MAVEN_SETTINGS_XML`. Remove 7.2.x/3.0.x comments and job-level `continue-on-error`.

- [x] **Step 5: Run the build contract and verify GREEN**

  Execute the focused Task 1 test with Maven 3. Expected: PASS.

- [x] **Step 6: Prove dependency resolution and full tests**

  Run the Task 2 dependency-tree and unit-test commands. Expected: only ddd4j `2.0.x.20260630-SNAPSHOT`, Javalin `7.1.0`, and zero test failures/errors.

### Task 4: Harden the Maven 4 `feature/7.2.x` line

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/integration-it.yml`
- Modify: `.mvn/wrapper/maven-wrapper.properties` only if it does not match the ddd4j 3.0.x wrapper
- Modify: POMs only when the contract finds a real Maven 3 residue

**Interfaces:**
- Consumes: ddd4j `3.0.x.20260630-SNAPSHOT`, Javalin `7.2.3`, Maven 4 wrapper.
- Produces: a JDK 21/Maven 4 branch with no production `<modules>` residue and branch-correct CI.

- [x] **Step 1: Apply the build contract and verify RED**

  Expected current failure: `integration-it.yml` configures JDK 17 instead of JDK 21. CI publication resolution may also fail while ddd4j 3.0.x remains unpublished.

- [x] **Step 2: Correct workflows**

  Use JDK 21 and `./mvnw` in both workflows, target only `feature/7.2.x`, consume `MAVEN_SETTINGS_XML`, and remove job-level `continue-on-error`.

- [x] **Step 3: Validate Maven 4 structure**

  ```bash
  rg -n '<modules>|<module>' --glob '**/pom.xml'
  rg -n '<subprojects>|<subproject>' --glob '**/pom.xml'
  find . -name pom.xml -print0 | xargs -0 -n1 xmllint --noout
  ```

  Expected: no actual Maven 3 aggregate elements; comments containing `<module>` are either clarified or excluded from the structural check. All POMs parse.

- [x] **Step 4: Run the contract and local reactor checks**

  ```bash
  ./mvnw -B -ntp -Denforcer.skip=true \
    -pl ddd4j-javalin-testcontainers -am \
    -DskipTests=false -Dsurefire.skip=false \
    -Dtest=BuildLineContractTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: configuration contract PASS. If parent resolution fails, record publication gate BLOCKED separately.

- [ ] **Step 5: Prove clean-repository consumption after upstream deploy**

  Run:

  ```bash
  maven_repo_dir="$(mktemp -d /tmp/ddd4j-javalin-m2.XXXXXX)"
  ./mvnw -U -B -ntp -Denforcer.skip=true \
    -Dmaven.repo.local="$maven_repo_dir" \
    -pl ddd4j-javalin-web -am dependency:tree \
    -Dincludes=io.ddd4j:*,io.javalin:javalin
  ```

  Then run the full Maven 4 unit suite with the same `-Dmaven.repo.local` value. Remove only that validated temporary directory after recording results.

### Task 5: Align Testcontainers fixtures with the module catalog

**Files:**
- Modify: `ddd4j-javalin-testcontainers/pom.xml`
- Modify: `ddd4j-javalin-testcontainers/src/main/java/io/ddd4j/javalin/testcontainers/**/*TestContainerFixture.java`
- Modify: `ddd4j-javalin-testcontainers/src/test/java/io/ddd4j/javalin/testcontainers/FixtureContractTest.java`
- Modify: affected `**/*IT.java`

**Interfaces:**
- Consumes: Testcontainers BOM 1.20.6, existing pinned images, official/community module classifications from `https://testcontainers.com/modules/`.
- Produces: centralized fixtures with pinned image tags and real consumer-facing round-trip assertions.

- [x] **Step 1: Write failing fixture contract cases**

  For each fixture, instantiate the real container type and assert its configured Docker image name literal. Add negative assertions that no fixture uses `latest` or an unqualified major-less tag. The test must fail if a production change selects the wrong image family/tag.

- [x] **Step 2: Verify RED for current gaps**

  Expected failures should identify any unpinned image, wrong container type, duplicated per-IT container configuration, or the SQS fixture still using a plain `GenericContainer` when `LocalStackContainer` is compatible with 1.20.6.

- [x] **Step 3: Apply minimal fixture corrections**

  Prefer module-specific Java container classes available in 1.20.6 for MySQL, PostgreSQL, MariaDB, MongoDB, Kafka, RabbitMQ, and LocalStack. Retain `GenericContainer` for services without a compatible 1.20.6 module wrapper. Keep the currently proven ARM64-compatible broker tags unless a focused container startup test proves they are invalid.

- [x] **Step 4: Verify fixture contract GREEN**

  On `feature/6.7.x` and `feature/7.1.x`, run:

  ```bash
  mvn -B -ntp -Denforcer.skip=true \
    -pl ddd4j-javalin-testcontainers -am \
    -DskipTests=false -Dsurefire.skip=false \
    -Dtest=FixtureContractTest test
  ```

  On `feature/7.2.x`, run the same arguments through `./mvnw`.

- [x] **Step 5: Audit disabled tests**

  Ensure ONS and TDMQ are explicitly disabled with managed-service reasons. Ensure Mica is either actually annotated `@Disabled` with a current issue reason or is executed; a comment saying it is disabled is insufficient.

### Task 6: Execute real container round-trips on each branch

**Files:**
- Test: `ddd4j-javalin-data/**/src/test/**/*IT.java`
- Test: `ddd4j-javalin-auth/**/src/test/**/*IT.java`
- Test: `ddd4j-javalin-mq/**/src/test/**/*IT.java`
- Test: `ddd4j-javalin-samples/ddd4j-javalin-sample-order-outbox/src/test/**/*IT.java`

**Interfaces:**
- Consumes: centralized fixtures from Task 5 and the matching line's ddd4j clients.
- Produces: per-service evidence for startup, request/command, persistence/broker side effect, response/consume, and cleanup.

- [ ] **Step 1: Run database and outbox IT**

  Run MySQL CRUD and PostgreSQL outbox modules with `-Pjavalin-integration-tests -am`. Expected: actual SQL write/read/transaction assertions, not only container startup.

- [x] **Step 2: Run auth IT**

  Run Sa-Token, Security, and Shiro Keycloak IT separately. Expected: token acquisition plus an allow/deny decision through the adapter.

- [x] **Step 3: Run broker IT serially**

  Execute Kafka, RabbitMQ, Artemis, RocketMQ, Pulsar, NATS, SQS, Redis Stream, and MQTT in separate Maven invocations. For each, require publish → broker → consume → acknowledgment assertions.

- [x] **Step 4: Record failures without blanket exemptions**

  Classify failures as code defect, image/platform incompatibility, upstream client defect, private artifact resolution, or transient infrastructure. Only a reproducible upstream defect may justify an individual `@Disabled`; never enable job-level `continue-on-error`.

### Task 7: Synchronize documentation and validation records

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture.md`
- Modify: `docs/javalin-version-matrix.md`
- Modify: `docs/testcontainers-guide.md`
- Create: `docs/superpowers/reports/2026-09-07-three-branch-convergence-status.md`
- Modify: this plan's checkbox/status and append a validation record

**Interfaces:**
- Consumes: actual final POMs, dependency trees, test reports, container runs, and CI URLs.
- Produces: source-backed documentation with no stale 6.3.x/7.2.2/2.0.x.20260730 claims.

- [x] **Step 1: Update version and Maven matrices**

  Document the exact matrix from Global Constraints and explicitly explain why 7.1.x is Maven 3 while 7.2.x is Maven 4.

- [x] **Step 2: Correct Testcontainers wording**

  Distinguish Testcontainers official modules, community modules, and images used through `GenericContainer`. Do not call every image “officially supported by Testcontainers.”

- [x] **Step 3: Record evidence by layer**

  For each branch record:

  - branch and commit SHA;
  - POM/model validation;
  - resolved ddd4j/Javalin versions;
  - unit test counts/failures/skips;
  - each container IT result;
  - GitHub Actions run URL and conclusion;
  - unresolved publication or platform blockers.

- [x] **Step 4: Run documentation consistency scan**

  ```bash
  rg -n 'feature/6\.3\.x|Javalin 7\.2\.2|2\.0\.x\.20260730|feature/7\.2\.x' \
    README.md docs .github/workflows
  ```

  Review every hit; retain only explicitly labeled history.

### Task 8: Final verification and remote execution gate

**Files:**
- No new production files; verification only.

**Interfaces:**
- Consumes: completed Tasks 1–7.
- Produces: final completion report or an explicit blocked report.

- [x] **Step 1: Run branch-local static gates**

  On each official branch run `git diff --check`, all-POM XML validation, forbidden Maven model/aggregate residue scan, and the focused build contract.

- [x] **Step 2: Run fresh full tests**

  Run the complete unit suite and all required Testcontainers IT with the line's prescribed Maven/JDK. Read complete summaries and count failures/errors/skips.

- [x] **Step 3: Obtain push authorization**

  Before pushing, show the exact local commits/files for all three branches and ask for explicit authorization. Do not infer push permission from implementation approval.

- [x] **Step 4: Trigger and wait for GitHub Actions**

  After authorized push, wait for the final SHA's CI and integration workflows. A workflow file existing locally is not completion evidence.

- [x] **Step 5: Compare local and remote SHAs**

  Compare each official branch against both configured remotes with `git ls-remote --heads`. Report any divergence without force-pushing.

- [ ] **Step 6: Complete the specification status only with evidence**

  Mark the spec implemented only when all mandatory gates pass. If ddd4j 3.0.x remains unpublished or a required CI job is red, leave the spec/plan incomplete and report the exact blocker.

## Validation Record

- 2026-09-07: Plan created from the approved design. No POM, production source, test, workflow, branch, commit, or remote was changed during planning.
- 2026-09-07 7.2.x RED: the build contract rejected workflows without XML validation and the IT workflow's JDK 17 configuration; the full suite also reproduced the shared fixed-port defect.
- 2026-09-07 7.2.x GREEN (local): all POMs retain `4.1.0/<subprojects>`, internal parents use Maven 4 relative-path inference, workflows use JDK 21/Maven 4, dependency tree resolves ddd4j `3.0.x.20260630-SNAPSHOT` and Javalin `7.2.3`, and the full local unit Reactor completed with zero failures/errors/skips.
- 2026-09-07 7.2.x Maven model correction: every source POM now uses matching 4.1.0 namespace/schema, and internal parents use relative-path inference without duplicate GAV. Maven still reports repeated imported-BOM conflicts originating from the deployed ddd4j 3.0.x model; these are upstream publication/model warnings, not closed by the local Javalin build.
- 2026-09-07 7.2.x publication gate: clean remote-consumption evidence remains blocked because the latest ddd4j 3.0.x Verify and Deploy runs are red. The populated local Maven repository is not accepted as publication proof.
- 2026-09-07 Testcontainers: added the Testcontainers 1.20.6 LocalStack module and centralized SQS fixture, replaced the SQS IT's duplicated GenericContainer setup, and expanded the fixture contract to 13 pinned-image cases. ONS/TDMQ remain managed-service exclusions; Mica remains explicitly disabled for the recorded upstream AIO defect.
- 2026-09-07 container verification: on all three branches MySQL CRUD, Sa-Token/Security/Shiro Keycloak, and nine broker round-trips passed. PostgreSQL outbox is not a common gate because its sample artifacts are unavailable on part of the upstream matrix.
- 2026-09-07 clean unit verification: 6.7.x ran 79 tests, 7.1.x ran 72 tests, and 7.2.x ran 66 tests; all had zero failures, errors, and skips.
- 2026-09-07 remote execution: all three branches were pushed to `origin` and `github`; both remotes matched the local SHA. All six GitHub Actions runs reached failure before starting any step because the account payment failed or the Actions spending limit must be increased. Remote code execution therefore remains blocked by account state rather than a repository test failure.
- 2026-09-08 CodeGraph correction: the three Keycloak tests are container-start smoke tests, not token/allow/deny IT; Task 6 Step 2 is reopened. The 6.7.x local web adapter does not invoke its injected request lifecycle, while the shared production bootstrap bypasses the complete core Guice module on all lines.

### Task 9: Phase A — close the production runtime and Web lifecycle

**Files:**
- Modify: `ddd4j-javalin-web/src/main/java/io/ddd4j/javalin/web/Ddd4jJavalinApplication.java`
- Modify: `ddd4j-javalin-web/src/main/java/io/ddd4j/javalin/web/Ddd4jJavalinAutoConfiguration.java`
- Modify: `ddd4j-javalin-web/src/main/java/io/ddd4j/javalin/web/Ddd4jJavalinProperties.java`
- Modify on 6.7.x: `ddd4j-javalin-web/src/main/java/io/ddd4j/javalin/web/Ddd4jJavalinWeb.java`
- Create/modify: `ddd4j-javalin-web/src/test/java/io/ddd4j/javalin/web/*ContractTest.java`

- [x] **Step 1: Extend the existing specification and plan with the CodeGraph findings**

  Keep `2026-09-07-three-branch-convergence-design.md` as the sole specification source. Reopen any historical
  checkbox whose expected behavior was not actually tested.

- [x] **Step 2: RED — protected request without token returns 401 on 6.7.x**

  Start a real random-port Javalin application with the production Web module, register a protected route and send
  a request without Authorization. Expected current result: 200, proving the injected lifecycle is unused.

- [x] **Step 3: GREEN — implement the Javalin 6 request lifecycle**

  Port the framework-neutral behavior from ddd4j's Javalin adapter using only Javalin 6 APIs. Preserve context open,
  authentication, Subject binding, translated errors, response IDs and success/failure cleanup.

- [x] **Step 4: RED/GREEN — install the complete core runtime from production bootstrap**

  Add a consumer-visible test that starts through `Ddd4jJavalinApplication` and resolves/uses CommandBus,
  DomainEventPublisher and projection SPI. Replace `MinimalSpiModule` only after the test fails for the expected reason.

- [x] **Step 5: RED/GREEN — align Web properties and idempotency with boot**

  Add independent behavior tests for authentication mode, trusted proxy selection, idempotency enable/disable/cache/TTL,
  request IDs, trace IDs, duplicate request conflict and cleanup. Remove or implement every currently unused property.

- [x] **Step 6: Apply compatible Phase A behavior to 7.1.x and 7.2.x**

  Reuse the upstream complete Javalin adapter on 7.x; change only the Guice composition, properties and tests. Preserve
  Maven 3/JDK 17 for 7.1.x and Maven 4/JDK 21 for 7.2.x.

- [x] **Step 7: Run focused and full verification on all three branches**

  Run Web lifecycle contracts, core contracts, full unit reactors and affected real HTTP integration tests separately.

### Task 10: Phase B — replace smoke and test doubles with real framework contracts

- [x] **Step 1: Add shared OIDC module contracts and reactor wiring**

  Add `ddd4j-javalin-auth-oidc` to auth aggregation and BOM on all three lines. Start with compile-failing tests for
  `OidcProperties`, `OidcTokenVerifier`, `OidcSubjectProvider` and `Ddd4jOidcJavalinModule`; add Nimbus only after the
  public API and security invariants are fixed by tests.

- [x] **Step 2: Verify JWT security and claim mapping**

  Use locally generated RSA/JWKS fixtures to prove signature, issuer, audience, exp, nbf, algorithm allowlist,
  bounded clock skew, roles/permissions mapping, invalid-token 401 and JWKS-unavailable 503. Do not use mocks for JWT verification.

- [x] **Step 3: Keycloak token and HTTP allow/deny on all lines**

  Obtain a real RS256 access token from the Testcontainers realm public client, call a protected Javalin route and
  assert Principal visibility plus 200. Missing/tampered token must return 401 and ThreadContext must be empty afterward.

- [x] **Step 4: Preserve native auth-provider boundaries**

  Keep Sa-Token/Security/Shiro native token tests separate. Rename their current Keycloak container-start tests to smoke
  semantics unless they actually install an OIDC bridge; do not claim the native providers directly validate Keycloak JWT.

- [x] **Step 5: MyBatis ddd4j Repository contract on MySQL**
- [x] **Step 6: JPA transaction commit/rollback on PostgreSQL**
- [x] **Step 7: DataScope, External and Data Logs consumer behavior**
- [ ] **Step 8: PostgreSQL Outbox as a three-line gate**

### Task 11: Phase C — durability and extension governance

- [ ] **Step 1: MQ persistence, ACK, retry, dead-letter and recovery tests**
- [ ] **Step 2: Decide every POM-only module: implement, direct-reuse proof, or removal**
- [ ] **Step 3: Re-run Testcontainers matrix and document explicit managed-service exclusions**
- [ ] **Step 4: Final local, private-repository and GitHub Actions evidence convergence**

#### Phase A Validation Record

- 2026-09-09 6.7.x RED: protected HTTP request returned 200 instead of 401 because the local Javalin 6 adapter never invoked `WebRequestLifecycle`.
- 2026-09-09 6.7.x dependency gate: upstream ddd4j 1.0.x build 17 consumer BOM stopped managing the standard `mybatis-plus-jsqlparser`; the Javalin JDK 17 line now centrally pins the upstream-compatible 3.5.9 version.
- 2026-09-09 6.7.x Web GREEN: real random-port HTTP contracts cover no-token 401, valid-token Subject binding, Request/Trace ID propagation, ThreadContext cleanup, configurable authentication mode, trusted forwarded client IP, duplicate idempotency 409 and disabled-idempotency behavior.
- 2026-09-09 6.7.x Runtime GREEN: production bootstrap installs the complete core Guice module, registers CommandBus, and closes the Guice runtime on Javalin stop.
- 2026-09-09 override contract: a business extra module can replace the default SubjectProvider through `Modules.override`; plain module ordering previously failed with duplicate bindings.
- 2026-09-09 idempotency configuration: custom cache names are registered, configured one-second TTL expires completed keys, and sub-second TTL is rejected because the core/Caffeine APIs only support whole seconds. The ddd4j core default Caffeine registration does not honor per-entry TTL, so Javalin configures global expire-after-write on its local fallback.
- 2026-09-09 final Phase A batch regression after override/TTL changes: 6.7.x ran 95 tests, 7.1.x ran 80 tests, and 7.2.x ran 74 tests; every clean reactor completed with zero failures/errors/skips.
- 2026-09-09 Phase A complete: explicit properties bootstrap now applies context path (including public-path mapping), CORS, max request size, global future timeout and lifecycle disablement. Final clean regressions ran 100/85/79 tests on 6.7.x/7.1.x/7.2.x with zero failures/errors/skips.
- 2026-09-09 Phase B OIDC Steps 1-3: added the shared `ddd4j-javalin-auth-oidc` reactor/BOM module on all three lines. Local RSA/JWKS tests cover issuer, audience, expiry, nbf, RS256 verification and allowlisted principal mapping; HTTP contracts cover public/protected paths, generic 401, provider-unavailable 503 and request-scope cleanup. A real Keycloak 26.2 token was obtained through the realm public client and accepted by protected Javalin 6.7.0, 7.1.0 and 7.2.3 routes. Each OIDC module ran 9 unit tests plus one Keycloak IT with zero failures/errors/skips. Full unit reactors passed on all three lines; upstream Maven 4 effective-model warnings on 7.2.x remain unchanged.
- 2026-09-09 Phase B auth boundary: renamed the Sa-Token, Spring Security and Shiro Keycloak checks to `*KeycloakSmokeIT` and removed references to a nonexistent sample bridge. These tests prove only container-fixture compatibility; only `ddd4j-javalin-auth-oidc` claims real token/JWKS/HTTP behavior.
- 2026-09-09 Phase B MyBatis RED/GREEN: replaced the hand-written repository and child injector with the real ddd4j `BaseRepositoryImpl` plus `bindRepository/initRepositories`. The first MySQL run failed because upstream `BaseRepositoryImpl` resolved generic index 0 as the domain type; ddd4j now overrides M/P/Q resolution at indexes 1/2/3 on all three lines. After local installation, the Javalin MySQL CRUD, query, pagination and delete contract passed.
- 2026-09-09 Phase B JPA RED/GREEN: added `JpaTransactionTemplate` and a real PostgreSQL RESOURCE_LOCAL contract. Successful callbacks commit, failing callbacks roll back, and every invocation closes its EntityManager. The JPA module accepts explicit provider properties and centrally pins Jakarta-compatible JAXB runtime 4.0.6 for Hibernate bootstrap.
- 2026-09-09 Phase B Step 7 RED/GREEN: DataScope now injects the consumer policy, defaults fail-closed, and proves allow/deny decisions. External binds caller properties, requires explicit Snowflake node IDs when consumed, and supports an injectable offline/real `IpRegionTemplate`. Data Logs proves mutually exclusive success/failure callbacks with a consumer provider; the upstream aspect no longer double-records failures. The invalid empty `Ddd4jLogsJavalinModule` source was removed.
- 2026-09-09 6.7.x regression: the 52-module clean unit reactor passed with 91 tests, zero failures/errors/skips. Custom idempotency cache/TTL and unused server-property decisions remain open in Task 9 Step 5.
- 2026-09-09 7.1.x compatibility: retained Maven 3/JDK 17 and the upstream Javalin 7 adapter; the full clean reactor passed 77 tests with zero failures/errors/skips (`3a0ea69`).
- 2026-09-09 7.2.x compatibility: retained Maven 4/POM 4.1.0/JDK 21 and the upstream Javalin 7 adapter; the full clean reactor passed 71 tests with zero failures/errors/skips (`ffecb3e`).
