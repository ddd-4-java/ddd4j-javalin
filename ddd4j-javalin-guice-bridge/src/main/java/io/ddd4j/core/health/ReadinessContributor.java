package io.ddd4j.core.health;

/**
 * 应用是否可接收业务流量的框架无关检查 SPI。
 *
 * <p>数据库、缓存和消息中间件等基础设施由应用按需实现本接口。Runtime 仅负责把
 * {@link ReadinessReport} 映射到各自的健康检查端点，不向核心层泄漏具体连接客户端。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public interface ReadinessContributor {

    /**
     * 执行一次就绪检查。
     *
     * @return 当前依赖的就绪结果
     */
    ReadinessResult check();
}
