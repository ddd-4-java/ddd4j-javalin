package io.ddd4j.javalin.sample.richmodel.order;

import com.google.inject.AbstractModule;
import io.ddd4j.sample.richmodel.order.application.OrderApplicationService;
import io.ddd4j.sample.richmodel.order.domain.repository.OrderRepository;
import io.ddd4j.sample.richmodel.order.infrastructure.persistence.InMemoryOrderRepository;

/**
 * Guice module for the rich-model sample.
 */
public class JavalinRichModelModule extends AbstractModule {

    @Override
    protected void configure() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        bind(OrderRepository.class).toInstance(repository);
        bind(OrderApplicationService.class).toInstance(new OrderApplicationService(repository));
    }
}
