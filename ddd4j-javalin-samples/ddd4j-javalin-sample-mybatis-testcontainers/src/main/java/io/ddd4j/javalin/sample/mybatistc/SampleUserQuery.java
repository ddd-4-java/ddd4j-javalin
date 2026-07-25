package io.ddd4j.javalin.sample.mybatistc;

/**
 * Sample query placeholder. In a real domain this would extend ddd4j-core's
 * {@code Query<T>} contract; the sample keeps it intentionally trivial.
 */
public record SampleUserQuery(String username) {
}