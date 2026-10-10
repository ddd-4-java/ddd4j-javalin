package io.ddd4j.javalin.testcontainers;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * WireMock 容器冒烟集成测试：验证 {@link WireMockTestContainerFixture} 能真实拉起
 * 钉版 {@code wiremock/wiremock:3.5.4} 容器，并完成「REST admin 注册 stub → 容器
 * 按 stub 返回响应」的最小往返。
 *
 * <p>此前 {@link WireMockTestContainerFixture} 只被 {@link FixtureContractTest}
 * 构造（不启动），没有任何用例真正 start 容器；本测试补齐该集成点。
 *
 * <p>收集口径：文件名 {@code *IT} —— 默认 {@code mvn test}（surefire 默认 includes
 * 为 <code>**&#47;*Test.java</code>）不收集；{@code -Pjavalin-integration-tests} 下由根 POM
 * profile 的 failsafe（<code>**&#47;*IT.java</code>）收集执行。
 *
 * <p>Docker daemon 不可达时由 {@code @Testcontainers(disabledWithoutDocker = true)}
 * 优雅跳过。
 */
// 测试镜像来源: https://testcontainers.com/modules/wiremock/
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class WireMockContainerIT {

    // 静态 @Container：容器随测试类启动一次、结束后由 TestcontainersExtension 回收
    @Container
    private static final GenericContainer<?> WIREMOCK = new WireMockTestContainerFixture().newContainer();

    @Test
    void shouldServeStubRegisteredViaAdminApi() throws Exception {
        String base = "http://" + WIREMOCK.getHost() + ":"
                + WIREMOCK.getMappedPort(WireMockTestContainerFixture.DEFAULT_PORT);
        HttpClient client = HttpClient.newHttpClient();

        // 1. 通过 REST admin 注册 stub：GET /smoke -> 200 "pong"
        String stub = "{\"request\":{\"method\":\"GET\",\"url\":\"/smoke\"},"
                + "\"response\":{\"status\":200,\"body\":\"pong\","
                + "\"headers\":{\"Content-Type\":[\"text/plain\"]}}}";
        HttpResponse<String> registered = client.send(
                HttpRequest.newBuilder(URI.create(base + "/__admin/mappings"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(stub))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        // WireMock 对成功创建的 mapping 返回 201 Created
        assertEquals(201, registered.statusCode(), "stub 注册应返回 201，实际: " + registered.body());

        // 2. 请求业务路径，容器应按 stub 返回
        HttpResponse<String> served = client.send(
                HttpRequest.newBuilder(URI.create(base + "/smoke")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, served.statusCode());
        assertEquals("pong", served.body());
    }
}
