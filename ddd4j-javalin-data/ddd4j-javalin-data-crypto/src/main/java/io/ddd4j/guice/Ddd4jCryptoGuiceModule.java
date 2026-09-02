package io.ddd4j.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.ddd4j.data.crypto.CryptoProperties;
import io.ddd4j.data.crypto.strategy.CryptoStrategy;
import io.ddd4j.data.crypto.strategy.DefaultCryptoStrategy;
import jakarta.inject.Singleton;

/**
 * ddd4j 加解密的 Guice 桥接模块。
 * <p>
 * 提供 {@link CryptoStrategy} 和 {@link CryptoProperties} 的 Guice 绑定，
 * 业务方 install 此模块后即可注入使用加解密能力。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public class Ddd4jCryptoGuiceModule extends AbstractModule {

    /**
     * 提供加解密配置属性。
     *
     * @return CryptoProperties 实例
     */
    @Provides
    @Singleton
    public CryptoProperties cryptoProperties() {
        return new CryptoProperties();
    }

    /**
     * 提供默认加解密策略。
     *
     * @param properties 加解密配置属性
     * @return CryptoStrategy 实例
     */
    @Provides
    @Singleton
    public CryptoStrategy defaultCryptoStrategy(CryptoProperties properties) {
        return new DefaultCryptoStrategy(null);
    }
}
