package io.ddd4j.core.cache;

import java.util.Objects;

import java.util.function.Consumer;

/**
 * 缓存配置（纯 Java 值对象，零框架依赖）。
 *
 * <p>描述缓存的各项参数，由 {@link CacheManager} 在创建缓存时使用。
 * 使用 Builder 模式构建：
 * <pre>{@code
 *   CacheConfig config = CacheConfig.builder()
 *       .maximumSize(10000)
 *       .expireAfterWriteSeconds(300)
 *       .expireAfterAccessSeconds(600)
 *       .refreshAfterWriteSeconds(60)
 *       .cacheType(CacheType.BOTH)
 *       .recordStats(true)
 *       .build();
 *   Cache<String, User> cache = cacheManager.getOrCreateCache("user", config);
 * }</pre>
 *
 * <p>各实现层（ddd4j-cache-core 的 JetCacheAdapter、CaffeineCache 等）
 * 将本配置转换为其底层引擎的具体配置。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public final class CacheConfig {

    /**
     * 默认最大容量
     */
    public static final long DEFAULT_MAXIMUM_SIZE = 1000L;

    /**
     * 缓存名称（业务标识）
     */
    private final String name;
    /**
     * 最大容量
     */
    private final long maximumSize;
    /**
     * 写后过期时间（秒），0 表示不过期
     */
    private final long expireAfterWriteSeconds;
    /**
     * 访问后过期时间（秒），0 表示不过期
     */
    private final long expireAfterAccessSeconds;
    /**
     * 写后刷新时间（秒），0 表示不自动刷新
     */
    private final long refreshAfterWriteSeconds;
    /**
     * 初始容量
     */
    private final int initialCapacity;
    /**
     * 是否记录统计信息
     */
    private final boolean recordStats;
    /**
     * 缓存类型
     */
    private final CacheType cacheType;
    /**
     * 本地缓存限制（多级缓存时本地缓存的最大条目数）
     */
    private final int localLimit;
    /**
     * 是否同步本地缓存（多级缓存时远程变更后是否广播同步本地）
     */
    private final boolean syncLocal;
    /**
     * 移除监听器（键级回调）
     */
    private final transient Consumer<String> removalListener;

    private CacheConfig(Builder builder) {
        this.name = builder.name;
        this.maximumSize = builder.maximumSize;
        this.expireAfterWriteSeconds = builder.expireAfterWriteSeconds;
        this.expireAfterAccessSeconds = builder.expireAfterAccessSeconds;
        this.refreshAfterWriteSeconds = builder.refreshAfterWriteSeconds;
        this.initialCapacity = builder.initialCapacity;
        this.recordStats = builder.recordStats;
        this.cacheType = builder.cacheType;
        this.localLimit = builder.localLimit;
        this.syncLocal = builder.syncLocal;
        this.removalListener = builder.removalListener;
    }

    /**
     * 创建 Builder。
     *
     * @param name 缓存名称（业务标识，不可为空）
     * @return Builder 实例
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * 创建 Builder（从已有配置复制）。
     *
     * @param source 源配置
     * @return Builder 实例
     */
    public static Builder builder(CacheConfig source) {
        return new Builder(source);
    }

    // ==================== Getters ====================

    public String getName() {
        return name;
    }

    public long getMaximumSize() {
        return maximumSize;
    }

    public long getExpireAfterWriteSeconds() {
        return expireAfterWriteSeconds;
    }

    public long getExpireAfterAccessSeconds() {
        return expireAfterAccessSeconds;
    }

    public long getRefreshAfterWriteSeconds() {
        return refreshAfterWriteSeconds;
    }

    public int getInitialCapacity() {
        return initialCapacity;
    }

    public boolean isRecordStats() {
        return recordStats;
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public int getLocalLimit() {
        return localLimit;
    }

    public boolean isSyncLocal() {
        return syncLocal;
    }

    public Consumer<String> getRemovalListener() {
        return removalListener;
    }

    // ==================== Builder ====================

    /**
     * 缓存配置构建器。
     */
    public static final class Builder {

        private final String name;
        private long maximumSize = DEFAULT_MAXIMUM_SIZE;
        private long expireAfterWriteSeconds = 0L;
        private long expireAfterAccessSeconds = 0L;
        private long refreshAfterWriteSeconds = 0L;
        private int initialCapacity = 0;
        private boolean recordStats = false;
        private CacheType cacheType = CacheType.LOCAL;
        private int localLimit = 100;
        private boolean syncLocal = false;
        private Consumer<String> removalListener = null;

        /**
         * 构造器（指定缓存名称）。
         *
         * @param name 缓存名称（业务标识）
         */
        public Builder(String name) {
            if (Objects.isNull(name) || io.ddd4j.kit.lang.StrKit.isEmpty(name)) {
                throw new IllegalArgumentException("缓存名称不能为空");
            }
            this.name = name;
        }

        /**
         * 构造器（从已有配置复制）。
         *
         * @param source 源配置
         */
        public Builder(CacheConfig source) {
            this.name = source.name;
            this.maximumSize = source.maximumSize;
            this.expireAfterWriteSeconds = source.expireAfterWriteSeconds;
            this.expireAfterAccessSeconds = source.expireAfterAccessSeconds;
            this.refreshAfterWriteSeconds = source.refreshAfterWriteSeconds;
            this.initialCapacity = source.initialCapacity;
            this.recordStats = source.recordStats;
            this.cacheType = source.cacheType;
            this.localLimit = source.localLimit;
            this.syncLocal = source.syncLocal;
            this.removalListener = source.removalListener;
        }

        /**
         * 设置最大容量。
         */
        public Builder maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        /**
         * 设置写后过期时间（秒）。
         */
        public Builder expireAfterWriteSeconds(long seconds) {
            this.expireAfterWriteSeconds = seconds;
            return this;
        }

        /**
         * 设置访问后过期时间（秒）。
         */
        public Builder expireAfterAccessSeconds(long seconds) {
            this.expireAfterAccessSeconds = seconds;
            return this;
        }

        /**
         * 设置写后刷新时间（秒）。
         */
        public Builder refreshAfterWriteSeconds(long seconds) {
            this.refreshAfterWriteSeconds = seconds;
            return this;
        }

        /**
         * 设置初始容量。
         */
        public Builder initialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
            return this;
        }

        /**
         * 设置是否记录统计信息。
         */
        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        /**
         * 设置缓存类型。
         */
        public Builder cacheType(CacheType cacheType) {
            this.cacheType = cacheType;
            return this;
        }

        /**
         * 设置本地缓存限制（多级缓存时本地缓存最大条目数）。
         */
        public Builder localLimit(int localLimit) {
            this.localLimit = localLimit;
            return this;
        }

        /**
         * 设置是否同步本地缓存（多级缓存时远程变更是否广播同步本地）。
         */
        public Builder syncLocal(boolean syncLocal) {
            this.syncLocal = syncLocal;
            return this;
        }

        /**
         * 设置移除监听器。
         */
        public Builder removalListener(Consumer<String> removalListener) {
            this.removalListener = removalListener;
            return this;
        }

        /**
         * 构建不可变的 CacheConfig 实例。
         *
         * @return CacheConfig 实例
         */
        public CacheConfig build() {
            return new CacheConfig(this);
        }

    }

}
