package io.ddd4j.javalin.data.panache;

import java.io.Serializable;

/**
 * 数据库自增 ID 策略：生成时返回 {@code null}，由数据库在 insert 时填充主键。
 *
 * <p>适用于数据库自增主键场景。
 * 使用此策略时，实体主键应标注：
 * <!-- 样例行首 @ 会被解析为 doc 块标签，必须转义为 &#64; -->
 * <pre>
 *   &#64;Id
 *   &#64;GeneratedValue(strategy = GenerationType.IDENTITY)
 *   public Long id;
 * </pre>
 *
 * <p>本策略主要用于编程式生成场景下的占位（表示"由 DB 决定"），
 * 实际持久化由 ORM 的 {@code IDENTITY} 策略完成。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 7.1.x
 */
public class AutoIncrementIdStrategy implements IdGenerationStrategy<Long> {

    /**
     * 生成下一个主键值；数据库自增策略返回 {@code null}。
     * @return 固定返回 {@code null}，由数据库自增填充。
     */
    @Override
    public Long generate() {
        // 返回 null 表示由数据库自增列填充
        return null;
    }
}
