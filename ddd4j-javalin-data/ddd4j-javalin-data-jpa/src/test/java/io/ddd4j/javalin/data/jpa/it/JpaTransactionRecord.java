package io.ddd4j.javalin.data.jpa.it;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA 事务测试记录，保存事务内写入的标识与值。
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 7.1.x
 */
@Entity
@Table(name = "jpa_transaction_record")
@Getter
@NoArgsConstructor
public class JpaTransactionRecord {

    @Id
    private String id;

    private String value;

    /**
     * 构造 JpaTransactionRecord 实例并初始化依赖。
     * @param id 标识值。
     * @param value 值。
     */
    public JpaTransactionRecord(String id, String value) {
        this.id = id;
        this.value = value;
    }
}
