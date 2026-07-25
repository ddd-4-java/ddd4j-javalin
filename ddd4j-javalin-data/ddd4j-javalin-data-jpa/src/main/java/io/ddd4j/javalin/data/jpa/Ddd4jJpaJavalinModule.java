package io.ddd4j.javalin.data.jpa;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

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

    public Ddd4jJpaJavalinModule(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    public Ddd4jJpaJavalinModule() {
        this("ddd4j-pu");
    }

    @Override
    protected void configure() {
        bind(EntityManagerFactory.class).toInstance(buildEntityManagerFactory());
        bind(EntityManagerFactory.class).in(Singleton.class);
    }

    private EntityManagerFactory buildEntityManagerFactory() {
        return Persistence.createEntityManagerFactory(persistenceUnitName);
    }
}