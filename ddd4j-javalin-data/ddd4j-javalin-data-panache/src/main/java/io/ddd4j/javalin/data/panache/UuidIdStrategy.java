package io.ddd4j.javalin.data.panache;

import io.ddd4j.kit.lang.IdKit;

import java.io.Serializable;

/**
 * UUID 字符串 ID 策略：生成 32 位无横线 UUID。
 *
 * <p>委托 ddd4j 标准工具 {@link IdKit#simpleUUID()}（Hutool 实现，比 JDK UUID 高性能）。
 * 适用于需要全局唯一、不依赖数据库自增、无安全顺序泄露要求的场景。
 * 使用此策略时，实体主键应为 {@code String} 类型：
 * <!-- 样例行首 @ 会被解析为 doc 块标签，必须转义为 &#64; -->
 * <pre>
 *   &#64;Id
 *   &#64;GeneratedValue(generator = "ddd4j-uuid")
 *   &#64;GenericGenerator(name = "ddd4j-uuid", strategy = "org.hibernate.id.UUIDGenerator")
 *   public String id;
 * </pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see IdKit
 * @since 7.1.x
 */
public class UuidIdStrategy implements IdGenerationStrategy<String> {

    /**
     * 生成下一个 UUID 主键。
     * @return 32 位无横线 UUID 字符串。
     */
    @Override
    public String generate() {
        return IdKit.simpleUUID();
    }
}
