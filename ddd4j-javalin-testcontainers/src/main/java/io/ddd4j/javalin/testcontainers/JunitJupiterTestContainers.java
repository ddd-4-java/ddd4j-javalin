package io.ddd4j.javalin.testcontainers;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenience annotation combining {@link org.testcontainers.junit.jupiter.Testcontainers} with
 * {@link Ddd4jTestContainersExtension} for skip-when-no-docker behaviour.
 *
 * <p>Use on integration test classes together with {@link org.testcontainers.junit.jupiter.Container}
 * static fields.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(Ddd4jTestContainersExtension.class)
public @interface JunitJupiterTestContainers {
}