package io.ddd4j.javalin.data.crypto;

import io.ddd4j.guice.data.crypto.Ddd4jCryptoGuiceModule;

/**
 * ddd4j-javalin 加解密 Guice Module。
 *
 * <p>注册加解密策略 Bean 到 Guice 容器，替代 Spring Boot 的 auto-config。
 * 业务项目可覆盖 DefaultCryptoStrategy，注册自定义 CryptoStrategy。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Deprecated
public class Ddd4jCryptoJavalinModule extends Ddd4jCryptoGuiceModule {
}
