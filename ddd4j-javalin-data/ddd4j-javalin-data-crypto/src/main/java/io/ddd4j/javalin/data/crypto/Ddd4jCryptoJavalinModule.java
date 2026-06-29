package io.ddd4j.javalin.data.crypto;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.ddd4j.data.crypto.CryptoProperties;
import io.ddd4j.data.crypto.strategy.CryptoStrategy;
import io.ddd4j.data.crypto.strategy.DefaultCryptoStrategy;

import jakarta.inject.Singleton;

/**
 * ddd4j-javalin 加解密 Guice Module。
 *
 * <p>注册加解密策略 Bean 到 Guice 容器，替代 Spring Boot 的 auto-config。
 * 业务项目可覆盖 DefaultCryptoStrategy，注册自定义 CryptoStrategy。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jCryptoJavalinModule extends AbstractModule {

    @Provides
    @Singleton
    public CryptoProperties cryptoProperties() {
        return new CryptoProperties();
    }

    @Provides
    @Singleton
    public CryptoStrategy defaultCryptoStrategy(CryptoProperties properties) {
        return new DefaultCryptoStrategy(null);
    }

}
