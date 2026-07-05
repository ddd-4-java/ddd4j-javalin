package io.ddd4j.sample.javalin.mq.disruptor.spi;

import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;

/**
 * 认证主体提供者：返回空（匿名）Subject 的示例实现。
 *
 * <p>真实应用应注入基于 sa-token / shiro / spring-security 的 SubjectProvider。
 * 本示例只演示 SPI 注册流程，不参与鉴权逻辑。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class AnonymousSubjectProvider implements SubjectProvider {

    @Override
    public Subject getSubject() {
        return null; // 匿名：未登录
    }
}
