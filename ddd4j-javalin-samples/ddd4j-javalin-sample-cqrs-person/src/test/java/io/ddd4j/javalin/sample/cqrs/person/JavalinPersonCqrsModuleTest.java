package io.ddd4j.javalin.sample.cqrs.person;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.ddd4j.core.cqrs.readmodel.ProjectionRunner;
import io.ddd4j.guice.Ddd4jGuiceModule;
import io.ddd4j.sample.cqrs.person.application.PersonCommandService;
import io.ddd4j.sample.cqrs.person.domain.CreatePersonCommand;
import io.ddd4j.sample.cqrs.person.domain.DeletePersonCommand;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.query.PersonListView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavalinPersonCqrsModuleTest {

    @Test
    void shouldWirePersonCqrsFlowWithGuice() {
        Injector injector = Guice.createInjector(
                new Ddd4jGuiceModule(),
                new JavalinPersonCqrsModule()
        );
        PersonCommandService commandService = injector.getInstance(PersonCommandService.class);
        PersonListView view = injector.getInstance(PersonListView.class);
        ProjectionRunner<PersonEvent> runner =
                injector.getInstance(Key.get(new TypeLiteral<ProjectionRunner<PersonEvent>>() {
                }));

        commandService.create(CreatePersonCommand.builder()
                .personId("p-200")
                .name("Bob")
                .build());
        runner.runOnce(view);
        assertEquals("Bob", view.findById("p-200").orElseThrow().getName());

        commandService.delete(DeletePersonCommand.builder()
                .personId("p-200")
                .build());
        runner.runOnce(view);
        assertTrue(view.findById("p-200").isEmpty());
    }
}
