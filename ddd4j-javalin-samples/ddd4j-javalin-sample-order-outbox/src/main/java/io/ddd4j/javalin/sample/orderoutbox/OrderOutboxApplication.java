package io.ddd4j.javalin.sample.orderoutbox;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.web.Ddd4jJavalinApplication;
import io.javalin.Javalin;

/**
 * Order Outbox 示例的 Javalin 启动入口。
 *
 * <p>用 {@link Ddd4jJavalinApplication#run(String[], String, com.google.inject.Module...)}
 * 一行完成 Guice 装配 + Javalin 启动（含 ddd4j 统一请求生命周期：认证、异常翻译、健康端点）；
 * 随后把示例业务路由（{@link OrderController}）注册到已启动的 Javalin 实例上，
 * 并启动 {@link OrderOutboxScheduler} 周期发布事务 Outbox 中的待发送事件。
 *
 * <p>注意：{@code run} 内部创建的 Guice Injector 不对外暴露，因此示例从同一个
 * {@link OrderOutboxGuiceModule} 实例构建第二个 Injector 来解析 controller 与 scheduler；
 * 模块共享同一组内存适配器（订单存储/Outbox/读模型），两个 Injector 之间状态一致。
 */
public class OrderOutboxApplication {

    /** 示例包基础名（Guice 注解扫描与 Javalin 启动共用）。 */
    private static final String BASE_PACKAGE = "io.ddd4j.javalin.sample.orderoutbox";

    /**
     * 默认构造方法：启动类仅含静态 {@code main} 入口，无实例状态。
     */
    public OrderOutboxApplication() {
    }

    /**
     * 启动示例：装配 Guice + Javalin，注册订单路由并启动 Outbox 周期调度。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        OrderOutboxGuiceModule module = new OrderOutboxGuiceModule();
        Javalin app = Ddd4jJavalinApplication.run(args, BASE_PACKAGE, module);

        Injector injector = Guice.createInjector(module);
        injector.getInstance(OrderController.class).register(app);
        injector.getInstance(OrderOutboxScheduler.class).start();
    }
}
