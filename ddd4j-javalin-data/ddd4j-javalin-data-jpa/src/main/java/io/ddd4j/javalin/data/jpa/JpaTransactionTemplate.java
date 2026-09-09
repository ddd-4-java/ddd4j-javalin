package io.ddd4j.javalin.data.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/** RESOURCE_LOCAL JPA 事务模板，每次调用独占并关闭一个 EntityManager。 */
public final class JpaTransactionTemplate {

    private final EntityManagerFactory entityManagerFactory;
    private final AtomicInteger openEntityManagers = new AtomicInteger();

    public JpaTransactionTemplate(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(
                entityManagerFactory, "entityManagerFactory must not be null");
    }

    /** 在新事务中执行回调；成功提交，异常回滚并原样抛出。 */
    public <T> T execute(Function<EntityManager, T> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        openEntityManagers.incrementAndGet();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            T result = callback.apply(entityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException | Error exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
            openEntityManagers.decrementAndGet();
        }
    }

    /** 仅供健康检查与测试观察是否存在未关闭的 EntityManager。 */
    public boolean hasOpenEntityManager() {
        return openEntityManagers.get() > 0;
    }

}
