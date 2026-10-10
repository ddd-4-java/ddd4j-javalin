package io.ddd4j.javalin.mq.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

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

/**
 * 扫描应用包内的 MQEventListener 并从 Guice 获取监听器实例。
 */
public final class JavalinMqListenerScanner {

    /**
     * 扫描并返回顺序稳定、route 唯一的监听器。
     *
     * @param injector    提供监听器实例的 Guice Injector，不允许为 {@code null}
     * @param basePackages 待扫描的基础包名集合，不允许为 {@code null}；空白项会被忽略
     * @return 按类型与方法签名稳定排序、route 不重复的监听器列表
     * @throws IllegalStateException 当出现重复 route 时抛出
     */
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

    private final static class ListenerMethod {

        private static final long serialVersionUID = 0L;

        private final Class<?> type;

        private final Method method;

        private final MQEventListener annotation;

        @JsonCreator()
        private ListenerMethod(@JsonProperty("type") Class<?> type, @JsonProperty("method") Method method, @JsonProperty("annotation") MQEventListener annotation) {
            this.type = type;
            this.method = method;
            this.annotation = annotation;
        }

        /**
         * 返回监听器方法声明类。
         *
         * @return 监听器方法声明类
         */
        @JsonProperty("type")
        public Class<?> type() {
            return type;
        }

        /**
         * 返回被 {@link MQEventListener} 标注的监听方法。
         *
         * @return 被 {@link MQEventListener} 标注的监听方法
         */
        @JsonProperty("method")
        public Method method() {
            return method;
        }

        /**
         * 返回 {@link MQEventListener} 注解实例。
         *
         * @return {@link MQEventListener} 注解实例
         */
        @JsonProperty("annotation")
        public MQEventListener annotation() {
            return annotation;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            ListenerMethod other = (ListenerMethod) obj;
            return Objects.equals(this.type, other.type) && Objects.equals(this.method, other.method) && Objects.equals(this.annotation, other.annotation);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(type);
            result = 31 * result + Objects.hashCode(method);
            result = 31 * result + Objects.hashCode(annotation);
            return result;
        }

        @Override
        public String toString() {
            return "ListenerMethod[type=" + type + ", method=" + method + ", annotation=" + annotation + "]";
        }
    }
}
