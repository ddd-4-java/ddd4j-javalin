package io.ddd4j.javalin.testcontainers.messaging;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Shared RocketMQ container fixture (namesrv + broker single container).
 *
 * <p>Uses the official {@code apache/rocketmq:5.3.2} image (multi-arch: native arm64).
 * Since 5.3.x images only start a single process per container, the fixture overrides the
 * command to boot namesrv and broker in one container, wiring {@code brokerIP1=127.0.0.1}
 * through a mounted {@code broker.conf} (auto-create topics enabled for tests).
 *
 * <p>Broker 10911 is bound to a fixed host port (via {@link FixedHostPortGenericContainer})
 * because the broker registers {@code 127.0.0.1:10911} with the namesrv — otherwise the
 * native client would dial the unbound host port 10911 and fail.
 *
 * <p>The {@code connectionString()} returns the {@code host:port} pair for the namesrv
 * because {@code ddd4j-javalin-mq-rocketmq} resolves the namesrv address.
 */
public class RocketMqTestContainerFixture extends AbstractTestContainerFixture<GenericContainer<?>> {

    public static final String DEFAULT_IMAGE = "apache/rocketmq:5.3.2";
    public static final int NAMESRV_PORT = 9876;
    public static final int BROKER_VIP_PORT = 10909;
    public static final int BROKER_PORT = 10911;
    public static final String BROKER_CONF = "/home/rocketmq/rocketmq-5.3.2/conf/broker.conf";

    @Override
    public GenericContainer<?> newContainer() {
        return new FixedHostPortGenericContainer<>(DEFAULT_IMAGE)
                .withFixedExposedPort(BROKER_PORT, BROKER_PORT)
                // 仅暴露 namesrv（9876 随机映射）+ 固定 10911；10909 为 proxy VIP 端口，
                // 无 proxy 时 broker 不监听，暴露会导致 Wait.forListeningPort() 永远超时
                .withExposedPorts(NAMESRV_PORT)
                // broker 默认大堆会 OOMKilled：收紧 namesrv+broker 的 JVM 堆
                .withEnv("JAVA_OPT_EXT", "-Xmx512m -Xms512m -Xmn128m")
                .withCopyFileToContainer(MountableFile.forClasspathResource("rocketmq/broker.conf"),
                        BROKER_CONF)
                // 5.3.x 镜像 entrypoint 只启动单进程：覆盖为 namesrv + broker 单容器双进程模式
                .withCommand("sh", "-c",
                        "sh mqnamesrv & sleep 10; sh mqbroker -n 127.0.0.1:9876 -c " + BROKER_CONF + " & wait")
                .waitingFor(Wait.forListeningPort())
                .withReuse(true);
    }

    @Override
    protected String resolveConnectionString(GenericContainer<?> container) {
        return String.format("%s:%d", container.getHost(),
                container.getMappedPort(NAMESRV_PORT));
    }
}
