package io.ddd4j.javalin.sample.mybatistc;

/**
 * Sample domain object (POJO / record). Kept minimal so the sample focuses on
 * MyBatis-Plus wiring rather than DDD aggregate plumbing.
 */
public record SampleUser(Long id, String username, String email) {
    public static SampleUser of(Long id, String username, String email) {
        return new SampleUser(id, username, email);
    }
}