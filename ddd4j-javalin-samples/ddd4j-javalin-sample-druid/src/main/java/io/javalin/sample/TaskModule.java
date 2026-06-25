package io.javalin.sample;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.javalin.sample.config.H2Config;
import io.javalin.sample.modules.DbModule;
import io.javalin.sample.modules.RedisModule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class TaskModule extends AbstractModule {
    private final TaskConfig config;

    ObjectMapper objectMapper = new ObjectMapper();

    public TaskModule(String configFile) {
        var configStr = FileUtil.readUtf8String(new File(configFile));
        try {
            config = objectMapper.readValue(configStr, TaskConfig.class);
        } catch (Exception e){
            log.error("Failed to parse config file", e);
        }

        //config = JSONObject.parseObject(configStr, TaskConfig.class);
        //startH2(config.getH2());
    }

    @SneakyThrows
    private void startH2(H2Config c) {
        var server = new Server();
        server.runTool(
            "-baseDir", c.getBaseDir(), "-ifNotExists",
            "-web", "-webAllowOthers", "-webPort", c.getWebPort().toString(), "-webExternalNames", c.getExtraNames(),
            "-tcp", "-tcpAllowOthers", "-tcpPort", c.getTcpPort().toString()
        );

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
    }

    @Override
    protected void configure() {
        bind(TaskConfig.class).toInstance(config);

        install(new DbModule(config.getDatabases()));
        install(new RedisModule(config.getRedis()));
    }

    @Provides
    @Singleton
    protected ExecutorService provideWorkers() {
        return Executors.newFixedThreadPool(config.getWorkers());
    }
}
