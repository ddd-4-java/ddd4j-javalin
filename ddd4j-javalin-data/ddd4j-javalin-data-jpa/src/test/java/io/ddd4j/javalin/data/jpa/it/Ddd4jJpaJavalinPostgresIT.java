package io.ddd4j.javalin.data.jpa.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.data.jpa.Ddd4jJpaJavalinModule;
import io.ddd4j.javalin.data.jpa.JpaTransactionTemplate;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.database.PostgresTestContainerFixture;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jJpaJavalinPostgresIT {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgresTestContainerFixture().newContainer();

    @Test
    void shouldCommitRollbackAndCloseEntityManagers() {
        POSTGRES.start();
        EntityManagerFactory factory = null;
        try {
            Injector injector = Guice.createInjector(new Ddd4jJpaJavalinModule(
                    "ddd4j-javalin-postgres",
                    Map.of("jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl(),
                            "jakarta.persistence.jdbc.user", POSTGRES.getUsername(),
                            "jakarta.persistence.jdbc.password", POSTGRES.getPassword(),
                            "jakarta.persistence.jdbc.driver", "org.postgresql.Driver",
                            "hibernate.hbm2ddl.auto", "create-drop")));
            factory = injector.getInstance(EntityManagerFactory.class);
            JpaTransactionTemplate transactions = injector.getInstance(JpaTransactionTemplate.class);

            transactions.execute(entityManager -> {
                entityManager.persist(new JpaTransactionRecord("commit", "visible"));
                return null;
            });
            assertThrows(IllegalStateException.class, () -> transactions.execute(entityManager -> {
                entityManager.persist(new JpaTransactionRecord("rollback", "hidden"));
                throw new IllegalStateException("force rollback");
            }));

            assertEquals(1L, count(transactions));
            assertFalse(transactions.hasOpenEntityManager());
            assertEquals(1L, count(transactions));

            try {
                transactions.execute(entityManager -> {
                    entityManager.persist(new JpaTransactionRecord("rollback", "hidden"));
                    throw new IllegalStateException("force rollback");
                });
            } catch (IllegalStateException expected) {
                assertEquals("force rollback", expected.getMessage());
            }
            assertEquals(1L, count(transactions));
            assertFalse(transactions.hasOpenEntityManager());
        } finally {
            if (factory != null) {
                factory.close();
            }
            POSTGRES.stop();
        }
    }

    private long count(JpaTransactionTemplate transactions) {
        return transactions.execute(entityManager -> entityManager
                .createQuery("select count(r) from JpaTransactionRecord r", Long.class)
                .getSingleResult());
    }
}
