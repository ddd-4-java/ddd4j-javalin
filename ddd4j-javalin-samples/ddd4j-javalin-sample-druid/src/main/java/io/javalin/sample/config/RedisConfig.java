package io.javalin.sample.config;

import lombok.Data;

@Data
public class RedisConfig {
    private String address;
    private Integer port;
    private String user = null;
    private String password = null;
    private Integer database = 0;
    private Integer minIdle = 2;
    private Integer maxIdle = 5;
    private Integer maxTotal = 10;
    private Long maxWaitMillis = -1L;
    private Long minEvictableIdleTimeMillis = 5 * 60000L;
    private Long timeBetweenEvictionRunsMillis = 60000L;
    private boolean testOnBorrow = false;
    private boolean testOnReturn = false;
    private boolean testWhileIdle = true;

}
