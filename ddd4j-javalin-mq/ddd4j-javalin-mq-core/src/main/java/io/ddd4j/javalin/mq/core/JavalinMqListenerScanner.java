package io.ddd4j.javalin.mq.core;

import com.google.inject.Injector;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.listener.MQListener;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 扫描应用包内的 MQEventListener 并从 Guice 获取监听器实例。 */
public final class JavalinMqListenerScanner {

    /** 扫描并返回顺序稳定、route 唯一的监听器。 */
    public List<MQListener> scan(Injector injector, Collection<String> basePackages) {
        Objects.requireNonNull(injector, "injector must not be null");
        Objects.requireNonNull(basePackages, "basePackages must not be null");
        String[] packages = basePackages.stream()
                .filter(StrKit::isNotBlank)
                .map(String::trim)
                .distinct()
                .toArray(String[]::new);
        if (packages.length == 0) {
            return List.of();
        }
        List<ListenerMethod> methods = new ArrayList<>();
        try (ScanResult result = new ClassGraph()
                .enableClassInfo()
                .enableMethodInfo()
                .enableAnnotationInfo()
                .acceptPackages(packages)
                .scan()) {
            for (ClassInfo classInfo : result.getClassesWithMethodAnnotation(MQEventListener.class.getName())) {
                Class<?> type = classInfo.loadClass();
                if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                    continue;
                }
                for (Method method : type.getDeclaredMethods()) {
                    MQEventListener annotation = method.getAnnotation(MQEventListener.class);
                    if (Objects.nonNull(annotation)) {
                        methods.add(new ListenerMethod(type, method, annotation));
                    }
                }
            }
        }
        methods.sort(Comparator.comparing((ListenerMethod value) -> value.type().getName())
                .thenComparing(value -> value.method().toGenericString()));
        List<MQListener> listeners = new ArrayList<>();
        Set<String> routes = new HashSet<>();
        for (ListenerMethod value : methods) {
            Object bean = injector.getInstance(value.type());
            MQListener listener = MQListener.of(bean, value.method(), value.annotation());
            String separator = StrKit.isBlank(listener.getSeparator()) ? "." : listener.getSeparator();
            String route = listener.getGroup() + '|' + listener.getRouteExpression(separator);
            if (!routes.add(route)) {
                throw new IllegalStateException("Duplicate MQ route: " + listener.getRouteExpression(separator));
            }
            listeners.add(listener);
        }
        return List.copyOf(listeners);
    }

    private record ListenerMethod(Class<?> type, Method method, MQEventListener annotation) {
    }
}
