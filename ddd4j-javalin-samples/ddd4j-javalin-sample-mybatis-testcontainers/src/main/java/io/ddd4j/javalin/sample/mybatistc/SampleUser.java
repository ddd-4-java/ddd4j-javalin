package io.ddd4j.javalin.sample.mybatistc;

/**
 * Sample domain object (POJO / record). Kept minimal so the sample focuses on
 * MyBatis-Plus wiring rather than DDD aggregate plumbing.
 *
 * @param id 主键
 * @param username 用户名
 * @param email 邮箱
 */
public record SampleUser(Long id, String username, String email) {

    /**
     * 便捷工厂方法。
     *
     * @param id 主键
     * @param username 用户名
     * @param email 邮箱
     * @return 新建的 SampleUser
     */
    public static SampleUser of(Long id, String username, String email) {
        return new SampleUser(id, username, email);
    }
}