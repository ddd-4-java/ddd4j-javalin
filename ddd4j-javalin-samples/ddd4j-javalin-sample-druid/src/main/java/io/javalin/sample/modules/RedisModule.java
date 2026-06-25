package io.javalin.sample.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.javalin.sample.config.RedisConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

public class RedisModule extends AbstractModule {

    private final RedisConfig config;

    public RedisModule(RedisConfig config) {
        this.config = config;
    }

    @Provides
    @Singleton
    protected JedisPooled provideRedis() {
        var poolConfig = new GenericObjectPoolConfig<Connection>();
        // 设置获取连接的最大等待时间
        poolConfig.setMaxWaitMillis(config.getMaxWaitMillis());
        // 设置最大连接数
        poolConfig.setMaxTotal(config.getMaxTotal());
        // 设置最大空闲连接数
        poolConfig.setMaxIdle(config.getMaxIdle());
        // 设置最小空闲连接数
        poolConfig.setMinIdle(config.getMinIdle());
        // 设置获取连接时不进行连接验证(通过 PoolableObjectFactory.validateObject() 验证连接是否有效)
        poolConfig.setTestOnBorrow(config.isTestOnBorrow());
        // 设置退还连接时不进行连接验证(通过 PoolableObjectFactory.validateObject() 验证连接是否有效)
        poolConfig.setTestOnReturn(config.isTestOnReturn());
        // 设置连接空闲时进行连接验证
        poolConfig.setTestWhileIdle(config.isTestWhileIdle());
        // 设置连接被回收前的最大空闲时间
        poolConfig.setMinEvictableIdleTimeMillis(config.getMinEvictableIdleTimeMillis());
        // 设置检测线程的运行时间间隔
        poolConfig.setTimeBetweenEvictionRunsMillis(config.getTimeBetweenEvictionRunsMillis());
        // 设置检测线程每次检测的对象数
        poolConfig.setNumTestsPerEvictionRun(-1);

        var clientConfig = DefaultJedisClientConfig.builder()
            .user(config.getUser())
            .password(config.getPassword())
            .database(config.getDatabase())
            .build();
        var hostPort = new HostAndPort(config.getAddress(), config.getPort());
        return new JedisPooled(poolConfig, hostPort, clientConfig);
    }

}
