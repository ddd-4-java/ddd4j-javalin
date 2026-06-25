package io.javalin.sample.tasks;

import com.google.inject.Inject;
import io.javalin.sample.TaskConfig;
import io.javalin.sample.misc.TaskHolder;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@TaskHolder("coin")
public class CoinCacheTask extends AbstractTask {

    @Inject
    TaskConfig config;

    @Override
    public void run() {
        log.info("coin task run");
    }

}
