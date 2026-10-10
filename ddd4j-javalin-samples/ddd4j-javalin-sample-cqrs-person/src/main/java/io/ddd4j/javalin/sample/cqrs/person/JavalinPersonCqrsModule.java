package io.ddd4j.javalin.sample.cqrs.person;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.core.cqrs.readmodel.DefaultProjectionService;
import io.ddd4j.core.cqrs.readmodel.InMemoryProjectionPositionRepository;
import io.ddd4j.core.cqrs.readmodel.ProjectionRunner;
import io.ddd4j.sample.cqrs.person.application.PersonCommandService;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.infrastructure.InMemoryPersonEventStore;
import io.ddd4j.sample.cqrs.person.infrastructure.InMemoryPersonRepository;
import io.ddd4j.sample.cqrs.person.query.PersonListView;

/**
 * 人员示例 CQRS 的 Guice 装配模块。
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 7.1.x
 */
public class JavalinPersonCqrsModule extends AbstractModule {

    /**
     * 提供内存人员事件存储。
     * @return 事件存储实例。
     */
    @Provides
    @Singleton
    public InMemoryPersonEventStore personEventStore() {
        return new InMemoryPersonEventStore();
    }

    /**
     * 提供内存人员仓储。
     * @param eventStore 事件存储。
     * @return 仓储实例。
     */
    @Provides
    @Singleton
    public InMemoryPersonRepository personRepository(InMemoryPersonEventStore eventStore) {
        return new InMemoryPersonRepository(eventStore);
    }

    /**
     * 提供人员命令服务。
     * @param repository 仓储实例。
     * @return 命令服务实例。
     */
    @Provides
    @Singleton
    public PersonCommandService personCommandService(InMemoryPersonRepository repository) {
        return new PersonCommandService(repository);
    }

    /**
     * 提供人员读模型视图。
     * @return 读视图实例。
     */
    @Provides
    @Singleton
    public PersonListView personListView() {
        return new PersonListView();
    }

    /**
     * 提供人员投影执行器。
     * @param eventStore 事件存储。
     * @return 投影执行器实例。
     */
    @Provides
    @Singleton
    public ProjectionRunner<PersonEvent> personProjectionRunner(InMemoryPersonEventStore eventStore) {
        return new ProjectionRunner<>(
                new DefaultProjectionService(new InMemoryProjectionPositionRepository()),
                eventStore
        );
    }
}
