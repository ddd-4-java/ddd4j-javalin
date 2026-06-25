package io.javalin.sample.controller;

import com.google.inject.Singleton;
import io.javalin.sample.misc.Exchange;

@Singleton
public class TaskController {

    public void health(Exchange exchange) {
        exchange.ctx().result("ok");
    }

}
