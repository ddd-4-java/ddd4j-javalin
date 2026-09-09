package io.ddd4j.javalin.data.jpa;

import com.google.inject.AbstractModule;
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

    public Ddd4jJpaJavalinModule(String persistenceUnitName) {
        this(persistenceUnitName, Collections.emptyMap());
    }

    public Ddd4jJpaJavalinModule(String persistenceUnitName, Map<String, ?> properties) {
        this.persistenceUnitName = Objects.requireNonNull(
                persistenceUnitName, "persistenceUnitName must not be null");
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(properties, "properties must not be null")));
    }

    public Ddd4jJpaJavalinModule() {
        this("ddd4j-pu");
    }

    @Override
    protected void configure() {
        EntityManagerFactory factory = buildEntityManagerFactory();
        bind(EntityManagerFactory.class).toInstance(factory);
        bind(JpaTransactionTemplate.class).toInstance(new JpaTransactionTemplate(factory));
    }

    private EntityManagerFactory buildEntityManagerFactory() {
        return Persistence.createEntityManagerFactory(persistenceUnitName, properties);
    }
}
