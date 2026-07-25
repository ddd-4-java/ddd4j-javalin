package io.ddd4j.javalin.testcontainers.database;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared MongoDB container fixture.
 *
 * <p>Uses {@code mongo:7} exposing the default 27017 port. The connection string follows the
 * standard {@code mongodb://host:port} shape consumed by both the official Java driver and
 * Panache.
 */
public class MongoDbTestContainerFixture extends AbstractTestContainerFixture<MongoDBContainer> {

    public static final String DEFAULT_IMAGE = "mongo:7";

    @Override
    public MongoDBContainer newContainer() {
        return new MongoDBContainer(DockerImageName.parse(DEFAULT_IMAGE)).withReuse(true);
    }

    @Override
    protected String resolveConnectionString(MongoDBContainer container) {
        return container.getReplicaSetUrl();
    }
}