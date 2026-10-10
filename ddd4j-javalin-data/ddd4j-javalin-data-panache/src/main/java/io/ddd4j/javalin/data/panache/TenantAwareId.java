package io.ddd4j.javalin.data.panache;

import java.io.Serializable;
import java.util.Objects;

/**
 * 多租户复合主键值对象，由实体主键与租户标识组成，用于 {@link TenantAwareEntity} 等实体的 {@code @IdClass} 映射。
 * <p>
 * 对标 ddd4j-data 的 {@code BaseRepositoryImpl} 四泛型方案中的主键抽象。
 * </p>
 */
public class TenantAwareId implements Serializable {

    private Long id;
    private String tenantId;

    /**
     * 构造 TenantAwareId 实例。
     */
    public TenantAwareId() {
    }

    /**
     * 构造 TenantAwareId 实例并初始化依赖。
     * @param id 标识值。
     * @param tenantId 租户标识。
     */
    public TenantAwareId(Long id, String tenantId) {
        this.id = id;
        this.tenantId = tenantId;
    }

    /**
     * 创建租户感知标识。
     * @param id 标识值。
     * @param tenantId 租户标识。
     * @return 新建的实例。
     */
    public static TenantAwareId of(Long id, String tenantId) {
        return new TenantAwareId(id, tenantId);
    }

    /**
     * 判断与另一对象是否相等。
     * @param o 比较对象。
     * @return 相等返回 {@code true}，否则返回 {@code false}。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantAwareId that = (TenantAwareId) o;
        return Objects.equals(id, that.id) && Objects.equals(tenantId, that.tenantId);
    }

    /**
     * 返回对象哈希码。
     * @return 哈希码。
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId);
    }
}
