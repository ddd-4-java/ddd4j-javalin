package io.ddd4j.javalin.data.jpa;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Guice Module for JPA wiring in Javalin applications.
 *
 * <p>Aligned with ddd4j-boot's {@code ddd4j-boot-data-jpa}: provides an
 * {@link EntityManagerFactory} derived from a JPA persistence unit name. Consumers should
 * pass the unit name to {@link #Ddd4jJpaJavalinModule(String)}; the module binds a
 * singleton EMF and lets downstream modules inject it.
 *
 * <p>Example:
 * <pre>{@code
 * Guice.createInjector(new Ddd4jJpaJavalinModule("ddd4j-pu"));
 * }</pre>
 */
public class Ddd4jJpaJavalinModule extends AbstractModule {

    private final String persistenceUnitName;
    private final Map<String, Object> properties;
    private final EntityManagerFactory suppliedFactory;

    public Ddd4jJpaJavalinModule(String persistenceUnitName) {
        this(persistenceUnitName, Collections.emptyMap());
    }

    public Ddd4jJpaJavalinModule(String persistenceUnitName, Map<String, ?> properties) {
        this.persistenceUnitName = Objects.requireNonNull(
                persistenceUnitName, "persistenceUnitName must not be null");
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(properties, "properties must not be null")));
        this.suppliedFactory = null;
    }

    /** 使用调用方管理的 EntityManagerFactory。该工厂不会被 Javalin 关闭。 */
    public Ddd4jJpaJavalinModule(EntityManagerFactory factory) {
        this.persistenceUnitName = null;
        this.properties = Collections.emptyMap();
        this.suppliedFactory = Objects.requireNonNull(factory, "factory must not be null");
    }

    public Ddd4jJpaJavalinModule() {
        this("ddd4j-pu");
    }

    @Override
    protected void configure() {
        boolean ownsFactory = Objects.isNull(suppliedFactory);
        EntityManagerFactory factory = ownsFactory ? buildEntityManagerFactory() : suppliedFactory;
        bind(EntityManagerFactory.class).toInstance(factory);
        bind(JpaTransactionTemplate.class).toInstance(new JpaTransactionTemplate(factory));
        bind(JpaLifecycleParticipant.class).toInstance(new JpaLifecycleParticipant(factory, ownsFactory));
        Multibinder.newSetBinder(binder(), JavalinLifecycleParticipant.class)
                .addBinding().to(JpaLifecycleParticipant.class);
    }

    private EntityManagerFactory buildEntityManagerFactory() {
        return Persistence.createEntityManagerFactory(persistenceUnitName, properties);
    }
}
