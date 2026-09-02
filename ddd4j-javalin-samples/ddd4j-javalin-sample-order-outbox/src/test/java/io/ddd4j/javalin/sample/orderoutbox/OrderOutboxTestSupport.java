package io.ddd4j.javalin.sample.orderoutbox;

import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.subject.AuthPrincipal;
import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;

/**
 * 集成测试公共支撑：向 ddd4j 全局 SPI 注册测试用内存 Subject，并提供可用于
 * {@code Authorization: Bearer <token>} 请求头的会话凭证。
 *
 * <p>ddd4j-javalin 统一请求生命周期默认 {@code AuthenticationMode.REQUIRED}
 * （非公开路径必须携带有效 Bearer Token），与 ddd4j-boot-sample-order 的
 * {@code OrderControllerTest} 使用同一套登录流程。
 *
 * <p>1.0.x 改挂：2.0.x 的 {@code io.ddd4j.cache.subject.InMemorySubject/InMemorySubjectProvider}
 * 编译期引用 1.0.x core 中已不存在的 {@code io.ddd4j.core.auth.*}（AuthRequest/AuthPrincipal），
 * 类链接不安全，故改用本测试内纯 1.0.x 契约的桩实现（{@link StubSubject}）。
 */
final class OrderOutboxTestSupport {

    /** 测试会话固定 Bearer Token（与 {@link StubSubject} 的放行令牌一致）。 */
    private static final String TEST_TOKEN = "order-outbox-it-token";

    /** 已登录会话的 Bearer 请求头值。 */
    static final String AUTHORIZATION = "Bearer " + TEST_TOKEN;

    static {
        Contexts.register(SpiKeys.SUBJECT_PROVIDER, SubjectProvider.class, new StubSubjectProvider());
    }

    private OrderOutboxTestSupport() {
    }

    /** 测试桩 Provider：每次返回独立的桩 Subject。 */
    private static final class StubSubjectProvider implements SubjectProvider {

        @Override
        public Subject getSubject() {
            return new StubSubject();
        }
    }

    /** 测试桩 Subject：认证放行指定 token，权限/角色全放行。 */
    private static final class StubSubject implements Subject {

        private AuthPrincipal principal() {
            return new AuthPrincipal()
                    .setLoginId("order-outbox-it-user")
                    .setUserId("order-outbox-it-user")
                    .setRoleCode("user");
        }

        @Override
        public <T extends AuthPrincipal> T getPrincipal() {
            return (T) principal();
        }

        @Override
        public <T extends AuthPrincipal> T getPrincipalByLoginId(Object loginId) {
            return (T) principal();
        }

        @Override
        public <T extends AuthPrincipal> T getPrincipalByToken(String token) {
            return TEST_TOKEN.equals(token) ? (T) principal() : null;
        }

        @Override
        public boolean isPermitted(String permission) {
            return true;
        }

        @Override
        public boolean isPermitted(Object loginId, String permission) {
            return true;
        }

        @Override
        public boolean[] isPermitted(String... permissions) {
            boolean[] result = new boolean[permissions.length];
            java.util.Arrays.fill(result, true);
            return result;
        }

        @Override
        public boolean[] isPermitted(Object loginId, String... permissions) {
            return isPermitted(permissions);
        }

        @Override
        public boolean isPermittedAny(String... permissions) {
            return true;
        }

        @Override
        public boolean isPermittedAny(Object loginId, String... permissions) {
            return true;
        }

        @Override
        public boolean isPermittedAll(String... permissions) {
            return true;
        }

        @Override
        public boolean isPermittedAll(Object loginId, String... permissions) {
            return true;
        }

        @Override
        public boolean hasRole(String role) {
            return true;
        }

        @Override
        public boolean hasRole(Object loginId, String role) {
            return true;
        }

        @Override
        public boolean[] hasRoles(String... roles) {
            boolean[] result = new boolean[roles.length];
            java.util.Arrays.fill(result, true);
            return result;
        }

        @Override
        public boolean[] hasRoles(Object loginId, String... roles) {
            return hasRoles(roles);
        }

        @Override
        public boolean hasAnyRole(String... roles) {
            return true;
        }

        @Override
        public boolean hasAnyRole(Object loginId, String... roles) {
            return true;
        }

        @Override
        public boolean hasAllRole(String... roles) {
            return true;
        }

        @Override
        public boolean hasAllRole(Object loginId, String... roles) {
            return true;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public boolean isAuthenticated(Object loginId) {
            return true;
        }

        @Override
        public boolean isRemembered() {
            return false;
        }

        @Override
        public boolean isTrustDeviceId(String deviceId) {
            return false;
        }

        @Override
        public boolean isTrustDeviceId(Object loginId, String deviceId) {
            return false;
        }
    }
}
