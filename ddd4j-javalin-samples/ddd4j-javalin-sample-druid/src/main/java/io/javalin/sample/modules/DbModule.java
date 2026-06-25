package io.javalin.sample.modules;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.sample.config.JdbcConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
public class DbModule extends AbstractModule {
    private final Map<String, JdbcConfig> dbConfMap;

    @SneakyThrows
    public DbModule(Map<String, JdbcConfig> confMap) {
        Class.forName(com.mysql.cj.jdbc.Driver.class.getName());
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");
        dbConfMap = confMap;
    }

    @Override
    protected void configure() {
        for(var conf : dbConfMap.entrySet()) {
            bind(DSLContext.class)
                    .annotatedWith(Names.named(conf.getKey()))
                    .toInstance(createJooqContext(conf.getValue()));
        }
    }

    private DSLContext createJooqContext(JdbcConfig config) {
        log.info("初始化数据库：{}", config.getUrl());

        var settings = new Settings();
        settings.setRenderCatalog(false);
        settings.setRenderSchema(false);

        var c = new DefaultConfiguration()
                .set(settings)
                .set(createDatasource(config))
                .set(config.getDialect());

        return new DefaultDSLContext(c);
    }

    private DataSource createDatasource(JdbcConfig conf) {
        var config = new HikariConfig();
        config.setJdbcUrl(conf.getUrl());
        config.setUsername(conf.getUser());
        config.setPassword(conf.getPass());
        //config.setThreadFactory(Thread.ofVirtual().factory());
        config.setConnectionTestQuery(conf.getConnectionTestQuery());
        config.setMaxLifetime(conf.getMaxLifetime());
        config.setMaximumPoolSize(conf.getMaxPoolSize());
        config.setMinimumIdle(conf.getMinIdle());
        config.setIdleTimeout(conf.getIdleTimeout());
        return new HikariDataSource(config);
    }
}
