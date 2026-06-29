package io.ddd4j.javalin.cache;

import com.google.inject.AbstractModule;
import io.ddd4j.cache.CacheKit;

/**
 * ddd4j-javalin 缓存 Guice Module。
 *
 * <p>{@link CacheKit} 是静态工具类（private 构造器），无需注册为 Bean。
 * 本 Module 仅提供配置占位，业务项目通过 {@code CacheKit.get(biz, key)} 静态方法使用。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jCacheJavalinModule extends AbstractModule {

    /**
     * 设置默认本地缓存类型。
     *
     * @param defaultType 默认缓存类型（CAFFEINE / GUAVA / HUTOOL）
     */
    public void setDefaultType(CacheKit.LocalCacheType defaultType) {
        CacheKit.setDefaultType(defaultType);
    }

}
