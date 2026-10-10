package io.ddd4j.sample.javalin.mq.rabbitmq.spi;

import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;

/**
 * 认证主体提供者：返回空（匿名）Subject 的示例实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class AnonymousSubjectProvider implements SubjectProvider {

    /**
     * 返回请求作用域的认证主体。
     * @return 认证主体。
     */
    @Override
    public Subject getSubject() {
        return null;
    }
}
