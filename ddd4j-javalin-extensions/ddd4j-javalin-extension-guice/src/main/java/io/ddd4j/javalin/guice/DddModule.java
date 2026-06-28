package io.ddd4j.javalin.guice;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.core.context.I18nKit;
import io.ddd4j.core.context.I18nProvider;
import io.ddd4j.core.contract.DomainEventPublisher;
import io.ddd4j.core.util.SubjectKit;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.javalin.guice.event.GuiceDomainEventPublisher;
import io.ddd4j.javalin.guice.i18n.GuiceI18nProvider;
import io.ddd4j.javalin.guice.subject.GuiceSubjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DDD Guice 模块：绑定核心 API 接口到 Guice 实现
 * <p>
 * 使用方式：
 * <pre>
 * Guice.createInjector(new DddModule(), ...);
 * </pre>
 *
 * @author Loong Wan
 */
public class DddModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(DddModule.class);

    private final String defaultLang;

    public DddModule() {
        this("zh");
    }

    public DddModule(String defaultLang) {
        this.defaultLang = defaultLang;
    }

    @Override
    protected void configure() {
        // 绑定 I18nProvider
        bind(I18nProvider.class).toInstance(new GuiceI18nProvider(defaultLang));

        // 绑定 SubjectProvider
        bind(SubjectProvider.class).to(GuiceSubjectProvider.class);

        // 注册到全局静态工具类
        I18nKit.register(new GuiceI18nProvider(defaultLang));
        logger.info("Registered I18nProvider for Guice");
    }

    @Provides
    @Singleton
    public EventBus provideEventBus() {
        return new EventBus();
    }

    @Provides
    @Singleton
    public DomainEventPublisher provideDomainEventPublisher(EventBus eventBus) {
        GuiceDomainEventPublisher publisher = new GuiceDomainEventPublisher(eventBus);
        // 注册到全局静态工具类
        SubjectKit.register(getProvider(SubjectProvider.class).get());
        logger.info("Registered SubjectProvider for Guice");
        return publisher;
    }
}
