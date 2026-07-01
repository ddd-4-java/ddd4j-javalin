package io.ddd4j.javalin.sample.cqrs.person;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.core.cqrs.projection.DefaultProjectionService;
import io.ddd4j.core.cqrs.projection.InMemoryProjectionPositionRepository;
import io.ddd4j.core.cqrs.projection.ProjectionRunner;
import io.ddd4j.sample.cqrs.person.application.PersonCommandService;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.infrastructure.InMemoryPersonEventStore;
import io.ddd4j.sample.cqrs.person.infrastructure.InMemoryPersonRepository;
import io.ddd4j.sample.cqrs.person.query.PersonListView;

public class JavalinPersonCqrsModule extends AbstractModule {

    @Provides
    @Singleton
    public InMemoryPersonEventStore personEventStore() {
        return new InMemoryPersonEventStore();
    }

    @Provides
    @Singleton
    public InMemoryPersonRepository personRepository(InMemoryPersonEventStore eventStore) {
        return new InMemoryPersonRepository(eventStore);
    }

    @Provides
    @Singleton
    public PersonCommandService personCommandService(InMemoryPersonRepository repository) {
        return new PersonCommandService(repository);
    }

    @Provides
    @Singleton
    public PersonListView personListView() {
        return new PersonListView();
    }

    @Provides
    @Singleton
    public ProjectionRunner<PersonEvent> personProjectionRunner(InMemoryPersonEventStore eventStore) {
        return new ProjectionRunner<>(
                new DefaultProjectionService(new InMemoryProjectionPositionRepository()),
                eventStore
        );
    }
}
