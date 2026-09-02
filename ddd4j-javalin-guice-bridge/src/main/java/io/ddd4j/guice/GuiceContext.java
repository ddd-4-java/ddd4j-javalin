/*
 * Copyright 2017-2026 the original author hiwepy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ddd4j.guice.context;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Guice IoC 容器上下文（等价于 Spring 的 ApplicationContext）。
 * <p>
 * 静态持有 {@link Injector}，提供全局访问能力：
 * <ul>
 *   <li>{@link #getInstance(Class)} - 按类型获取实例</li>
 *   <li>{@link #getInstance(String, Class)} - 按名称获取实例</li>
 *   <li>{@link #getInstances(Class)} - 获取某类型的所有绑定实例</li>
 *   <li>{@link #getInjector()} - 获取底层 Injector</li>
 * </ul>
 * <p>
 * 在应用启动时调用 {@link #setInjector(Injector)} 注入。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
public class GuiceContext {

    /**
     * 初始化等待信号
     */
    private static volatile CountDownLatch initSignal = new CountDownLatch(1);
    /**
     * 自定义属性存储
     */
    private static final Map<String, Object> ATTRIBUTES = new ConcurrentHashMap<>();
    /**
     * Guice 注入器实例
     */
    private static volatile Injector injector;

    private GuiceContext() {
    }

    /**
     * 获取 Injector（阻塞等待初始化完成）
     */
    public static Injector getInjector() {
        if (Objects.isNull(injector)) {
            try {
                initSignal.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for GuiceContext initialization", e);
            }
        }
        return injector;
    }

    /**
     * 设置 Injector（应用启动时调用一次）
     */
    public static void setInjector(Injector inj) {
        if (Objects.nonNull(injector)) {
            log.warn("GuiceContext already initialized, overwriting existing Injector");
        }
        injector = inj;
        initSignal.countDown();
        log.info("GuiceContext initialized with Injector: {}", inj);
    }

    /**
     * 按类型获取实例
     */
    public static <T> T getInstance(Class<T> clazz) {
        return getInjector().getInstance(clazz);
    }

    /**
     * 按名称获取实例
     */
    public static <T> T getInstance(String name, Class<T> clazz) {
        return getInjector().getInstance(Key.get(clazz, Names.named(name)));
    }

    /**
     * 按注解获取实例
     */
    public static <T> T getInstance(Class<T> clazz, Class<? extends Annotation> annotationType) {
        return getInjector().getInstance(Key.get(clazz, annotationType));
    }

    /**
     * 获取某类型的所有绑定实例
     */
    public static <T> Collection<T> getInstances(Class<T> clazz) {
        List<T> instances = new ArrayList<>();
        Map<Key<?>, Binding<?>> bindings = getInjector().getAllBindings();
        for (Binding<?> binding : bindings.values()) {
            Class<?> rawType = binding.getKey().getTypeLiteral().getRawType();
            if (clazz.isAssignableFrom(rawType) && rawType != clazz) {
                try {
                    @SuppressWarnings("unchecked")
                    T instance = (T) getInjector().getInstance(binding.getKey());
                    instances.add(instance);
                } catch (Exception e) {
                    log.debug("Cannot instantiate binding: {}", binding.getKey(), e);
                }
            }
        }
        return instances;
    }

    /**
     * 获取环境属性
     */
    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * 获取环境属性（带默认值）
     */
    public static String getProperty(String key, String defaultValue) {
        // 优先从系统属性获取
        String value = System.getProperty(key);
        if (Objects.nonNull(value)) {
            return value;
        }
        // 再从环境变量获取
        value = System.getenv(key);
        if (Objects.nonNull(value)) {
            return value;
        }
        // 再从自定义属性获取
        Object attr = ATTRIBUTES.get(key);
        if (Objects.nonNull(attr)) {
            return attr.toString();
        }
        return defaultValue;
    }

    /**
     * 设置自定义属性
     */
    public static void setAttribute(String key, Object value) {
        ATTRIBUTES.put(key, value);
    }

    /**
     * 获取自定义属性
     */
    public static Object getAttribute(String key) {
        return ATTRIBUTES.get(key);
    }

    /**
     * 判断 Injector 是否已初始化
     */
    public static boolean isInitialized() {
        return Objects.nonNull(injector);
    }

    /**
     * 清理全局 Injector 与运行期属性，供运行时关闭和测试隔离使用。
     */
    public static synchronized void clear() {
        injector = null;
        ATTRIBUTES.clear();
        initSignal = new CountDownLatch(1);
    }

    /**
     * 获取注入器中所有绑定的类型
     */
    public static Set<Class<?>> getBoundTypes() {
        Set<Class<?>> types = new HashSet<>();
        Map<Key<?>, Binding<?>> bindings = getInjector().getAllBindings();
        for (Binding<?> binding : bindings.values()) {
            types.add(binding.getKey().getTypeLiteral().getRawType());
        }
        return types;
    }
}
