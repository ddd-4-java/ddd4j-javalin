package io.ddd4j.javalin.data.jpa.it;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jpa_transaction_record")
@Getter
@NoArgsConstructor
public class JpaTransactionRecord {

    @Id
    private String id;

    private String value;

    public JpaTransactionRecord(String id, String value) {
        this.id = id;
        this.value = value;
    }
}
