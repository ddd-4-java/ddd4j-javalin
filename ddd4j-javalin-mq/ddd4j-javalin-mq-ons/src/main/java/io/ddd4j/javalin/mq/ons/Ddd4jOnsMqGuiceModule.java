package io.ddd4j.javalin.mq.ons;

import com.aliyun.openservices.ons.api.Producer;
import io.ddd4j.mq.config.Ddd4jMQProperties;

import java.util.Properties;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.ons.Ddd4jOnsMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jOnsMqGuiceModule extends io.ddd4j.guice.mq.ons.Ddd4jOnsMqGuiceModule {

    public Ddd4jOnsMqGuiceModule(Producer producer, Properties onsConnectionProperties) {
        super(producer, onsConnectionProperties);
    }

    public Ddd4jOnsMqGuiceModule(Producer producer, Properties onsConnectionProperties, Ddd4jMQProperties mqProperties) {
        super(producer, onsConnectionProperties, mqProperties);
    }

}
