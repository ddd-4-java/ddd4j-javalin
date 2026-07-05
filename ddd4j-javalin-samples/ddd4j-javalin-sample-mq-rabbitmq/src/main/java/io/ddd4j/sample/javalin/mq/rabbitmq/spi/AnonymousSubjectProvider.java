package io.ddd4j.sample.javalin.mq.rabbitmq.spi;

import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;

/**
 * 认证主体提供者：返回空（匿名）Subject 的示例实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class AnonymousSubjectProvider implements SubjectProvider {

    @Override
    public Subject getSubject() {
        return null;
    }
}
