package io.ddd4j.javalin.testcontainers;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 ddd4j-javalin 三条发布线的构建契约。
 *
 * <p>测试从根 POM 的 revision 推导当前发布线，随后核对 ddd4j、Javalin、Maven
 * 模型、聚合元素、Wrapper、JDK 与 GitHub Actions 分支，防止不同发布线的构建
 * 语义被交叉复制。</p>
 */
class BuildLineContractTest {

    private static final Map<String, BuildLine> LINES = Map.of(
            "6.7.x.20260630-SNAPSHOT",
            new BuildLine("1.0.x.20260630-SNAPSHOT", "6.7.0", "4.0.0", "modules", "17", "3.9.16"),
            "7.1.x.20260630-SNAPSHOT",
            new BuildLine("2.0.x.20260630-SNAPSHOT", "7.1.0", "4.0.0", "modules", "17", "3.9.16"),
            "7.2.x.20260630-SNAPSHOT",
            new BuildLine("3.0.x.20260630-SNAPSHOT", "7.2.3", "4.1.0", "subprojects", "21", "4.0.0-rc-6"));

    /**
     * 校验当前发布线的版本与 Maven 聚合模型。
     */
    @Test
    void shouldMatchVersionAndMavenContractForCurrentLine() throws Exception {
        Path root = repositoryRoot();
        Document rootPom = parse(root.resolve("pom.xml"));
        String revision = property(rootPom, "revision");
        BuildLine expected = LINES.get(revision);

        assertTrue(LINES.containsKey(revision), "未登记的发布线 revision: " + revision);
        assertEquals(expected.ddd4jVersion(), parentVersion(rootPom), "根 parent 版本与发布线不匹配");
        assertEquals(expected.ddd4jVersion(), property(rootPom, "ddd4j.version"), "ddd4j.version 与发布线不匹配");
        assertEquals(expected.jdkVersion(), property(rootPom, "java.version"), "java.version 与发布线不匹配");
        assertEquals(expected.mavenVersion(), property(rootPom, "maven.version"), "maven.version 与 Wrapper 不匹配");
        assertEquals(expected.modelVersion(), text(rootPom, "modelVersion"), "根 POM modelVersion 不匹配");
        assertEquals(1, rootPom.getElementsByTagName(expected.aggregateElement()).getLength(), "根 POM 聚合元素不匹配");

        String forbiddenAggregate = "modules".equals(expected.aggregateElement()) ? "subprojects" : "modules";
        try (var paths = Files.walk(root)) {
            List<Path> poms = paths
                    .filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .toList();
            for (Path pom : poms) {
                Document document = parse(pom);
                assertEquals(expected.modelVersion(), text(document, "modelVersion"),
                        () -> "错误 Maven modelVersion: " + root.relativize(pom));
                assertEquals("http://maven.apache.org/POM/" + expected.modelVersion(),
                        document.getDocumentElement().getAttribute("xmlns"),
                        () -> "错误 Maven POM namespace: " + root.relativize(pom));
                assertTrue(document.getDocumentElement().getAttribute("xsi:schemaLocation")
                                .contains("maven-" + expected.modelVersion() + ".xsd"),
                        () -> "错误 Maven POM schema: " + root.relativize(pom));
                assertEquals(0, document.getElementsByTagName(forbiddenAggregate).getLength(),
                        () -> "出现不属于当前发布线的聚合元素: " + root.relativize(pom));
            }
        }
    }

    /**
     * 校验当前发布线只声明一个明确的 Javalin 运行版本。
     */
    @Test
    void shouldDeclareOneUnambiguousJavalinRuntimeVersion() throws Exception {
        Path root = repositoryRoot();
        Document rootPom = parse(root.resolve("pom.xml"));
        BuildLine expected = LINES.get(property(rootPom, "revision"));
        Document dependencies = parse(root.resolve("ddd4j-javalin-dependencies/pom.xml"));

        assertEquals(expected.javalinVersion(), property(dependencies, "javalin.version"));
        assertEquals(0, dependencies.getElementsByTagName("javalin6.version").getLength(),
                "禁止用第二个版本属性覆盖 javalin.version");
    }

    /**
     * Maven 4 的内部子项目通过相对路径推断父坐标，禁止同时重复声明父 GAV。
     */
    @Test
    void shouldUseMaven4ParentInferenceWithoutDuplicateCoordinates() throws Exception {
        Path root = repositoryRoot();
        Document rootPom = parse(root.resolve("pom.xml"));
        BuildLine expected = LINES.get(property(rootPom, "revision"));
        if (!"4.1.0".equals(expected.modelVersion())) {
            return;
        }

        try (var paths = Files.walk(root)) {
            List<Path> childPoms = paths
                    .filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .filter(path -> !path.equals(root.resolve("pom.xml")))
                    .filter(path -> !path.toString().contains("/target/"))
                    .toList();
            for (Path pom : childPoms) {
                Document document = parse(pom);
                Element parent = (Element) document.getElementsByTagName("parent").item(0);
                assertEquals(0, parent.getElementsByTagName("groupId").getLength(),
                        () -> "Maven 4 内部 parent 不应重复 groupId: " + root.relativize(pom));
                assertEquals(0, parent.getElementsByTagName("artifactId").getLength(),
                        () -> "Maven 4 内部 parent 不应重复 artifactId: " + root.relativize(pom));
                assertEquals(0, parent.getElementsByTagName("version").getLength(),
                        () -> "Maven 4 内部 parent 不应重复 version: " + root.relativize(pom));
            }
        }
    }

    /**
     * 校验 Maven Wrapper 与两个 GitHub Actions workflow 使用当前发布线的工具链。
     */
    @Test
    void shouldUseMatchingWrapperAndGithubActionsToolchain() throws Exception {
        Path root = repositoryRoot();
        Document rootPom = parse(root.resolve("pom.xml"));
        String revision = property(rootPom, "revision");
        BuildLine expected = LINES.get(revision);
        String branch = "feature/" + revision.substring(0, revision.indexOf(".20260630-SNAPSHOT"));

        String wrapper = Files.readString(root.resolve(".mvn/wrapper/maven-wrapper.properties"));
        assertTrue(wrapper.contains("/apache-maven/" + expected.mavenVersion() + "/"), "Maven Wrapper 版本不匹配");

        for (String workflowName : List.of("ci.yml", "integration-it.yml")) {
            String workflow = Files.readString(root.resolve(".github/workflows").resolve(workflowName));
            assertTrue(workflow.contains(branch), workflowName + " 未监听 " + branch);
            assertTrue(workflow.contains("java-version: '" + expected.jdkVersion() + "'"),
                    workflowName + " JDK 不匹配");
            assertTrue(workflow.contains("MAVEN_SETTINGS_XML: ${{ secrets.MAVEN_SETTINGS_XML }}"),
                    workflowName + " 未通过 env 消费 MAVEN_SETTINGS_XML");
            assertTrue(workflow.contains("size < 50"), workflowName + " 未拒绝空 settings.xml");
            assertTrue(workflow.contains("ElementTree.parse(target)"), workflowName + " 未解析校验 settings.xml");
            assertFalse(workflow.contains("continue-on-error: true"),
                    workflowName + " 不得整体忽略失败");
        }
    }

    /** 测试运行时必须提供唯一 SLF4J provider，避免静默降级为 NOP 日志。 */
    @Test
    void shouldProvideTestLoggingBinding() throws Exception {
        Document rootPom = parse(repositoryRoot().resolve("pom.xml"));
        assertEquals(1, dependencyCount(rootPom, "org.slf4j", "slf4j-simple", "test"),
                "测试运行时必须提供一个 SLF4J binding/provider");
    }

    /** 公共 BOM 必须直接继承运行时依赖管理，父级版本不能抢占本发布线的 Javalin 版本。 */
    @Test
    void shouldInheritRuntimeDependenciesInPublicBom() throws Exception {
        Document bom = parse(repositoryRoot().resolve("ddd4j-javalin-bom/pom.xml"));
        Element parent = (Element) bom.getElementsByTagName("parent").item(0);
        assertEquals("../ddd4j-javalin-dependencies/pom.xml", text(parent, "relativePath"),
                "ddd4j-javalin-bom 必须直接继承 ddd4j-javalin-dependencies");
        assertEquals(0, managedImportCount(bom, "io.ddd4j.javalin", "ddd4j-javalin-dependencies"),
                "直接父级不可再作为 BOM 重复 import");
    }

    @Test
    void shouldGovernEveryPomOnlyModuleExplicitly() throws Exception {
        Path root = repositoryRoot();
        Set<String> approvedPomOnly = Set.of(
                "ddd4j-javalin-auth", "ddd4j-javalin-bom", "ddd4j-javalin-data",
                "ddd4j-javalin-ddd", "ddd4j-javalin-dependencies", "ddd4j-javalin-extensions",
                "ddd4j-javalin-extensions/ddd4j-javalin-extension-excel",
                "ddd4j-javalin-extensions/ddd4j-javalin-extension-monitor",
                "ddd4j-javalin-extensions/ddd4j-javalin-extension-pf4j",
                "ddd4j-javalin-mq", "ddd4j-javalin-parent", "ddd4j-javalin-samples");
        try (var paths = Files.walk(root)) {
            Set<String> actual = paths.filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .filter(path -> !path.equals(root.resolve("pom.xml")))
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> {
                        Path sources = path.getParent().resolve("src/main/java");
                        if (!Files.isDirectory(sources)) {
                            return true;
                        }
                        try (var javaFiles = Files.walk(sources)) {
                            return javaFiles.noneMatch(file -> file.toString().endsWith(".java"));
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .map(path -> root.relativize(path.getParent()).toString())
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(approvedPomOnly, actual, "POM-only module governance inventory drifted");
            for (String module : actual) {
                assertEquals("pom", text(parse(root.resolve(module).resolve("pom.xml")), "packaging"),
                        "POM-only module must explicitly use pom packaging: " + module);
            }
        }
    }

    private static Path repositoryRoot() {
        String configuredRoot = System.getProperty("ddd4j.repo.root");
        Path start = Path.of(Objects.isNull(configuredRoot) ? "" : configuredRoot).toAbsolutePath().normalize();
        Path current = start;
        while (Objects.nonNull(current)
                && !(Files.isRegularFile(current.resolve("pom.xml"))
                && Files.isDirectory(current.resolve("ddd4j-javalin-testcontainers")))) {
            current = current.getParent();
        }
        if (Objects.isNull(current)) {
            throw new IllegalStateException("无法定位 ddd4j-javalin 根目录");
        }
        return current;
    }

    private static Document parse(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(pom.toFile());
    }

    private static String parentVersion(Document document) {
        Element parent = (Element) document.getElementsByTagName("parent").item(0);
        return text(parent, "version");
    }

    private static String property(Document document, String name) {
        Element properties = (Element) document.getElementsByTagName("properties").item(0);
        return text(properties, name);
    }

    private static String text(Document document, String name) {
        return document.getElementsByTagName(name).item(0).getTextContent().trim();
    }

    private static String text(Element element, String name) {
        NodeList values = element.getElementsByTagName(name);
        return values.item(0).getTextContent().trim();
    }

    private static int dependencyCount(Document document, String groupId, String artifactId, String scope) {
        int count = 0;
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            if (groupId.equals(text(dependency, "groupId"))
                    && artifactId.equals(text(dependency, "artifactId"))
                    && scope.equals(text(dependency, "scope"))) {
                count++;
            }
        }
        return count;
    }

    private static int managedImportCount(Document document, String groupId, String artifactId) {
        int count = 0;
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            if (groupId.equals(text(dependency, "groupId"))
                    && artifactId.equals(text(dependency, "artifactId"))
                    && "pom".equals(text(dependency, "type"))
                    && "import".equals(text(dependency, "scope"))) {
                count++;
            }
        }
        return count;
    }

    private record BuildLine(String ddd4jVersion,
                             String javalinVersion,
                             String modelVersion,
                             String aggregateElement,
                             String jdkVersion,
                             String mavenVersion) {
    }
}
