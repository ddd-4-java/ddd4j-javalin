package io.javalin.sample;

import io.javalin.sample.config.JdbcConfig;
import io.javalin.sample.config.RedisConfig;
import lombok.Data;

import java.util.Map;

@Data
public class TaskConfig {
    private Integer httpPort = 8090;

    private Map<String, String> tasks;
    private Map<String, JdbcConfig> databases;
    //排名的缓存时间
    private Long rankExpiresSec = 20 * 60L;

    private RedisConfig redis;

    //TVL, ROI统计里过滤token
    private String baseToken;
    private String quoteToken;

    //工作线程池线程数
    private Integer workers = 7;
}
