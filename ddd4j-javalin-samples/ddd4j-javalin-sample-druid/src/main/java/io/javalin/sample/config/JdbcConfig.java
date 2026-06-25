package io.javalin.sample.config;

import lombok.Data;
import org.jooq.SQLDialect;

@Data
public class JdbcConfig {
    private String url;
    private String user;
    private String pass;
    private SQLDialect dialect;
    /**
     * 连接测试查询
     */
    private String connectionTestQuery = "SELECT 1";
    /**
     * 连接超时时间:毫秒，小于250毫秒，否则被重置为默认值30秒
     */
    private Long connectionTimeout = 60000L;
    /**
     * 一个连接的最大的生命时间（毫秒），超时而且没被使用则被释放（retired），缺省:30分钟，建议设置比数据库超时时长少30秒以上，参考MySQL wait_timeout参数（show variables like '%timeout%';）
     */
    private Long maxLifetime = 1800000L;
    /**
     * 连接池中允许的最大连接数。缺省值：10；大于零小于1会被重置为minimum-idle的值；推荐的公式：((core_count * 2) + effective_spindle_count)
     */
    private Integer maxPoolSize = 10;
    /**
     * 最小空闲连接，默认值10，小于0或大于maximum-pool-size，都会重置为maximum-pool-size
     */
    private Integer minIdle = 2;
    /**
     * 空闲连接超时时间，默认值600000（10分钟），大于等于max-lifetime且max-lifetime>0，会被重置为0；不等于0且小于10秒，会被重置为10秒。
     */
    private Long idleTimeout = 600000L;

}
