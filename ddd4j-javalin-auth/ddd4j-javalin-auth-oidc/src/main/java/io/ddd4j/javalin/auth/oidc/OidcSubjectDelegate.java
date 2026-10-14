package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.core.subject.Subject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 写入 {@link ThreadContext#SUBJECT_KEY} 的只读 OIDC Subject 持有器。
 *
 * <p>仅承载已验证的 {@link AuthPrincipal}，所有 mutation 方法（{@code login/logout/...}）
 * 抛 {@link UnsupportedOperationException}，与外部身份提供方语义一致。</p>
 *
 * <p>{@link OidcSubject#currentPrincipal()} 通过 {@code instanceof} 识别本类，
 * 避免污染其它 {@link Subject} 实现持有的 ThreadContext 槽位。</p>
 */
final class OidcSubjectDelegate implements Subject {

    private final AuthPrincipal principal;
    private final OidcTokenVerifier verifier;

    OidcSubjectDelegate(AuthPrincipal principal, OidcTokenVerifier verifier) {
        this.principal = Objects.requireNonNull(principal, "principal must not be null");
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
    }

    AuthPrincipal principal() {
        return principal;
    }

    /**
     * 获取当前上下文绑定的认证主体。
     * @param <T> 类型参数。
     * @return 认证主体；未绑定或类型不匹配时返回 {@code null}。
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T getPrincipal() {
        return (T) principal;
    }

    /**
     * 按登录标识获取认证主体。
     * @param <T> 类型参数。
     * @param loginId 登录标识。
     * @return 匹配登录标识的认证主体；不匹配时返回 {@code null}。
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T getPrincipalByLoginId(Object loginId) {
        return Objects.equals(loginId, principal.getLoginId()) ? (T) principal : null;
    }

    /**
     * 按访问令牌获取认证主体。
     * @param <T> 类型参数。
     * @param tokenValue 访问令牌值。
     * @return 验证通过的认证主体。
     */
    @Override
    public <T extends AuthPrincipal> T getPrincipalByToken(String tokenValue) {
        return verify(tokenValue);
    }

    /**
     * 判断是否拥有指定权限。
     * @param permission 权限标识。
     * @return 权限判定结果。
     */
    @Override
    public boolean isPermitted(String permission) {
        Set<String> perms = Objects.isNull(principal.getPerms()) ? Collections.emptySet() : principal.getPerms();
        return perms.contains(permission);
    }

    /**
     * 判断是否拥有指定权限。
     * @param loginId 登录标识。
     * @param permission 权限标识。
     * @return 权限判定结果。
     */
    @Override
    public boolean isPermitted(Object loginId, String permission) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && isPermitted(permission);
    }

    /**
     * 判断是否拥有指定权限。
     * @param permissions 权限标识数组。
     * @return 权限判定结果。
     */
    @Override
    public boolean[] isPermitted(String... permissions) {
        boolean[] result = new boolean[permissions.length];
        for (int index = 0; index < permissions.length; index++) {
            result[index] = isPermitted(permissions[index]);
        }
        return result;
    }

    /**
     * 判断是否拥有指定权限。
     * @param loginId 登录标识。
     * @param permissions 权限标识数组。
     * @return 权限判定结果。
     */
    @Override
    public boolean[] isPermitted(Object loginId, String... permissions) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) ? isPermitted(permissions) : new boolean[permissions.length];
    }

    /**
     * 判断是否拥有任一指定权限。
     * @param permissions 权限标识数组。
     * @return 任一满足返回 {@code true}。
     */
    @Override
    public boolean isPermittedAny(String... permissions) {
        return Arrays.stream(permissions).anyMatch(this::isPermitted);
    }

    /**
     * 判断是否拥有任一指定权限。
     * @param loginId 登录标识。
     * @param permissions 权限标识数组。
     * @return 任一满足返回 {@code true}。
     */
    @Override
    public boolean isPermittedAny(Object loginId, String... permissions) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && isPermittedAny(permissions);
    }

    /**
     * 判断是否拥有全部指定权限。
     * @param permissions 权限标识数组。
     * @return 全部满足返回 {@code true}。
     */
    @Override
    public boolean isPermittedAll(String... permissions) {
        return Arrays.stream(permissions).allMatch(this::isPermitted);
    }

    /**
     * 判断是否拥有全部指定权限。
     * @param loginId 登录标识。
     * @param permissions 权限标识数组。
     * @return 全部满足返回 {@code true}。
     */
    @Override
    public boolean isPermittedAll(Object loginId, String... permissions) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && isPermittedAll(permissions);
    }

    /**
     * 判断是否拥有指定角色。
     * @param roleIdentifier 角色标识。
     * @return 角色判定结果。
     */
    @Override
    public boolean hasRole(String roleIdentifier) {
        List<AuthPrincipal.RolePair> roles = Objects.isNull(principal.getRoles()) ? Collections.emptyList() : principal.getRoles();
        return roles.stream().anyMatch(role -> roleIdentifier.equals(role.getRoleCode()) || roleIdentifier.equals(role.getRoleId()));
    }

    /**
     * 判断是否拥有指定角色。
     * @param loginId 登录标识。
     * @param roleIdentifier 角色标识。
     * @return 角色判定结果。
     */
    @Override
    public boolean hasRole(Object loginId, String roleIdentifier) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && hasRole(roleIdentifier);
    }

    /**
     * 逐个判断是否拥有指定角色。
     * @param roleIdentifiers 角色标识数组。
     * @return 角色判定结果数组，与入参顺序一致。
     */
    @Override
    public boolean[] hasRoles(String... roleIdentifiers) {
        boolean[] result = new boolean[roleIdentifiers.length];
        for (int index = 0; index < roleIdentifiers.length; index++) {
            result[index] = hasRole(roleIdentifiers[index]);
        }
        return result;
    }

    /**
     * 逐个判断是否拥有指定角色。
     * @param loginId 登录标识。
     * @param roleIdentifiers 角色标识数组。
     * @return 角色判定结果数组，与入参顺序一致。
     */
    @Override
    public boolean[] hasRoles(Object loginId, String... roleIdentifiers) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) ? hasRoles(roleIdentifiers) : new boolean[roleIdentifiers.length];
    }

    /**
     * 判断是否拥有任一指定角色。
     * @param roleIdentifiers 角色标识数组。
     * @return 任一满足返回 {@code true}。
     */
    @Override
    public boolean hasAnyRole(String... roleIdentifiers) {
        return Arrays.stream(roleIdentifiers).anyMatch(this::hasRole);
    }

    /**
     * 判断是否拥有任一指定角色。
     * @param loginId 登录标识。
     * @param roleIdentifiers 角色标识数组。
     * @return 任一满足返回 {@code true}。
     */
    @Override
    public boolean hasAnyRole(Object loginId, String... roleIdentifiers) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && hasAnyRole(roleIdentifiers);
    }

    /**
     * 判断是否拥有全部指定角色。
     * @param roleIdentifiers 角色标识数组。
     * @return 全部满足返回 {@code true}。
     */
    @Override
    public boolean hasAllRole(String... roleIdentifiers) {
        return Arrays.stream(roleIdentifiers).allMatch(this::hasRole);
    }

    /**
     * 判断是否拥有全部指定角色。
     * @param loginId 登录标识。
     * @param roleIdentifiers 角色标识数组。
     * @return 全部满足返回 {@code true}。
     */
    @Override
    public boolean hasAllRole(Object loginId, String... roleIdentifiers) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && hasAllRole(roleIdentifiers);
    }

    /**
     * 判断主体是否已认证。
     * @return 已认证返回 {@code true}。
     */
    @Override
    public boolean isAuthenticated() {
        return true;
    }

    /**
     * 判断主体是否已认证。
     * @param loginId 登录标识。
     * @return 已认证返回 {@code true}。
     */
    @Override
    public boolean isAuthenticated(Object loginId) {
        return Objects.nonNull(getPrincipalByLoginId(loginId));
    }

    /**
     * 判断主体是否处于记住登录状态。
     * @return 记住登录返回 {@code true}。
     */
    @Override
    public boolean isRemembered() {
        return false;
    }

    /**
     * 判断设备标识是否受信任。
     * @param deviceId 设备标识。
     * @return 受信任返回 {@code true}。
     */
    @Override
    public boolean isTrustDeviceId(String deviceId) {
        return false;
    }

    /**
     * 判断设备标识是否受信任。
     * @param userId 用户标识。
     * @param deviceId 设备标识。
     * @return 受信任返回 {@code true}。
     */
    @Override
    public boolean isTrustDeviceId(Object userId, String deviceId) {
        return false;
    }

    /**
     * 执行登录。
     * @param request 登录请求参数。
     * @return 登录成功返回登录标识。
     */
    @Override
    public String login(AuthRequest request) {
        throw readOnly();
    }

    /**
     * 执行登出。
     */
    @Override
    public void logout() {
        throw readOnly();
    }

    /**
     * 执行登出。
     * @param loginId 登录标识。
     */
    @Override
    public void logout(Object loginId) {
        throw readOnly();
    }

    /**
     * 强制下线指定登录标识。
     * @param loginId 登录标识。
     */
    @Override
    public void kickout(Object loginId) {
        throw readOnly();
    }

    /**
     * 刷新登录凭证。
     * @return 刷新成功返回新的登录标识。
     */
    @Override
    public String refresh() {
        throw readOnly();
    }

    /**
     * 验证令牌并返回认证主体。
     * @param <T> 类型参数。
     * @param token 访问令牌。
     * @return 验证通过的认证主体。
     * @throws OidcAuthenticationException 令牌无效时抛出。
     * @throws OidcProviderUnavailableException OIDC 提供方不可用时抛出。
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T verify(String token) {
        return (T) verifier.verify(token);
    }

    /**
     * 禁用指定登录标识一段时间。
     * @param loginId 登录标识。
     * @param timeout 禁用时长（毫秒）。
     */
    @Override
    public void disable(Object loginId, long timeout) {
        throw readOnly();
    }

    /**
     * 判断登录标识是否已被禁用。
     * @param loginId 登录标识。
     * @return 已禁用返回 {@code true}。
     */
    @Override
    public boolean isDisabled(Object loginId) {
        return false;
    }

    /**
     * 解除登录标识的禁用状态。
     * @param loginId 登录标识。
     */
    @Override
    public void untieDisable(Object loginId) {
        throw readOnly();
    }

    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("OIDC resource-server subject is read-only");
    }

}