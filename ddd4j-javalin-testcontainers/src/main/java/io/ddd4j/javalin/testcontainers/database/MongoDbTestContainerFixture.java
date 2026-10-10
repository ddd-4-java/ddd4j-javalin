package io.ddd4j.javalin.testcontainers.database;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared MongoDB container fixture.
 *
 * <p>Uses {@code mongo:7.0.43} exposing the default 27017 port. The connection string follows the
 * standard {@code mongodb://host:port} shape consumed by both the official Java driver and
 * Panache.
 */
public class MongoDbTestContainerFixture extends AbstractTestContainerFixture<MongoDBContainer> {

    /** 默认容器镜像（钉版；原浮动别名 7 与该补丁标签 digest 一致）。 */
    // 测试镜像来源: https://testcontainers.com/modules/mongodb/
    public static final String DEFAULT_IMAGE = "mongo:7.0.43";

    @Override
    public MongoDBContainer newContainer() {
        return new MongoDBContainer(DockerImageName.parse(DEFAULT_IMAGE)).withReuse(true);
    }

    @Override
    protected String resolveConnectionString(MongoDBContainer container) {
        return container.getReplicaSetUrl();
    }
}