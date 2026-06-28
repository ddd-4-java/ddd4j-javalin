package io.ddd4j.javalin.guice.subject;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice 实现的 Subject 提供者
 * <p>
 * 通过 Guice Injector 注入 Subject 实现，替代 Spring 的 ApplicationContext.getBean()。
 *
 * @author Loong Wan
 */
public class GuiceSubjectProvider implements SubjectProvider {

    private static final Logger logger = LoggerFactory.getLogger(GuiceSubjectProvider.class);

    private final Injector injector;

    @Inject
    public GuiceSubjectProvider(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Subject getSubject() {
        try {
            return injector.getInstance(Subject.class);
        } catch (Exception e) {
            logger.debug("No Subject implementation found in Guice container");
            return null;
        }
    }
}
