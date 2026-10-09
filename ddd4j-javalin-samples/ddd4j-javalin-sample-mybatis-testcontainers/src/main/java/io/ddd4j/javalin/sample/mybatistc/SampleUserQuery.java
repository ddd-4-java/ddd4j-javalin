package io.ddd4j.javalin.sample.mybatistc;

/**
 * Sample query placeholder. In a real domain this would extend ddd4j-core's
 * {@code Query<T>} contract; the sample keeps it intentionally trivial.
 *
 * @param username 用户名
 */
public record SampleUserQuery(String username) {
}