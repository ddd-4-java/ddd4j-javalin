package io.javalin.sample;

import cn.hutool.core.lang.ClassScanner;
import cn.hutool.cron.CronUtil;
import com.google.common.base.Preconditions;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.sample.misc.BizError;
import io.javalin.sample.misc.R;
import io.javalin.sample.tasks.AbstractTask;
import io.javalin.validation.ValidationException;
import io.prometheus.metrics.exporter.servlet.jakarta.PrometheusMetricsServlet;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.util.ArrayList;

@Slf4j
public class JavalinApplication {

    @Inject
    private Injector guice;

    @Inject
    private TaskConfig config;

    public static void main(String[] args) {
        Guice.createInjector(new TaskModule(args[0])).getInstance(JavalinApplication.class).start();
    }

    private void start() {
        startHttp();
        startTasks();
    }

    private void startHttp() {
        JvmMetrics.builder().register();

        var app = Javalin.create(c -> {
            c.router.apiBuilder(guice.getInstance(TaskRouter.class));
            c.showJavalinBanner = false;
            c.http.maxRequestSize = 200 * 1024 * 1024;
            c.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));
            c.jetty.modifyServer(server -> {
                //初始化普罗米修斯
                var context = new ServletContextHandler();
                context.setContextPath("/internal/metrics");
                context.addServlet(PrometheusMetricsServlet.class, "/");

                var handlers = new ContextHandlerCollection();
                handlers.addHandler(context);
                server.setHandler(handlers);
            });

        }).start(config.getHttpPort());

        app.exception(BizError.class, (bizError, ctx) -> {
            var resp = R.error(bizError.error);
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(resp);
        });

        app.exception(ValidationException.class, (validateError, ctx) -> {
            var errors = new ArrayList<String>();
            for (var it : validateError.getErrors().entrySet()) {
                for (var err : it.getValue()) {
                    errors.add(err.getMessage());
                }
            }

            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(
                R.error(HttpStatus.BAD_REQUEST.getCode())
                    .data(errors)
                    .message("validation error")
            );
        });

        app.exception(Exception.class, (error, ctx) -> {
            log.error("unknown error：", error);

            var code = HttpStatus.INTERNAL_SERVER_ERROR;
            var throwByGuava = error.getStackTrace()[0].getClassName().equals(Preconditions.class.getName());
            if (throwByGuava) {
                code = HttpStatus.BAD_REQUEST;
            }
            ctx.status(code);
            ctx.json(R.error(code.getCode()).message(error.getMessage()));
        });

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    private void startTasks() {
        var taskClasses = ClassScanner.scanPackageBySuper(this.getClass().getPackageName(), AbstractTask.class);

        for(var it : taskClasses) {
            var taskName = it.getSimpleName();
            if (config.getTasks().containsKey(taskName)) {
                var cronExpr = config.getTasks().get(taskName);
                CronUtil.schedule(taskName, cronExpr, (AbstractTask) guice.getInstance(it));

                log.info("调度任务: {} {}", taskName, cronExpr);
            }
        }

        CronUtil.getScheduler().setDaemon(true);
        CronUtil.start();

        Runtime.getRuntime().addShutdownHook(new Thread(CronUtil::stop));
    }
}
