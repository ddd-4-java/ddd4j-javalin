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

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T getPrincipal() {
        return (T) principal;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T getPrincipalByLoginId(Object loginId) {
        return Objects.equals(loginId, principal.getLoginId()) ? (T) principal : null;
    }

    @Override
    public <T extends AuthPrincipal> T getPrincipalByToken(String tokenValue) {
        return verify(tokenValue);
    }

    @Override
    public boolean isPermitted(String permission) {
        Set<String> perms = Objects.isNull(principal.getPerms()) ? Collections.emptySet() : principal.getPerms();
        return perms.contains(permission);
    }

    @Override
    public boolean isPermitted(Object loginId, String permission) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && isPermitted(permission);
    }

    @Override
    public boolean[] isPermitted(String... permissions) {
        boolean[] result = new boolean[permissions.length];
        for (int index = 0; index < permissions.length; index++) {
            result[index] = isPermitted(permissions[index]);
        }
        return result;
    }

    @Override
    public boolean[] isPermitted(Object loginId, String... permissions) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) ? isPermitted(permissions) : new boolean[permissions.length];
    }

    @Override
    public boolean isPermittedAny(String... permissions) {
        return Arrays.stream(permissions).anyMatch(this::isPermitted);
    }

    @Override
    public boolean isPermittedAny(Object loginId, String... permissions) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && isPermittedAny(permissions);
    }

    @Override
    public boolean isPermittedAll(String... permissions) {
        return Arrays.stream(permissions).allMatch(this::isPermitted);
    }

    @Override
    public boolean isPermittedAll(Object loginId, String... permissions) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && isPermittedAll(permissions);
    }

    @Override
    public boolean hasRole(String roleIdentifier) {
        List<AuthPrincipal.RolePair> roles = Objects.isNull(principal.getRoles()) ? Collections.emptyList() : principal.getRoles();
        return roles.stream().anyMatch(role -> roleIdentifier.equals(role.getRoleCode()) || roleIdentifier.equals(role.getRoleId()));
    }

    @Override
    public boolean hasRole(Object loginId, String roleIdentifier) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && hasRole(roleIdentifier);
    }

    @Override
    public boolean[] hasRoles(String... roleIdentifiers) {
        boolean[] result = new boolean[roleIdentifiers.length];
        for (int index = 0; index < roleIdentifiers.length; index++) {
            result[index] = hasRole(roleIdentifiers[index]);
        }
        return result;
    }

    @Override
    public boolean[] hasRoles(Object loginId, String... roleIdentifiers) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) ? hasRoles(roleIdentifiers) : new boolean[roleIdentifiers.length];
    }

    @Override
    public boolean hasAnyRole(String... roleIdentifiers) {
        return Arrays.stream(roleIdentifiers).anyMatch(this::hasRole);
    }

    @Override
    public boolean hasAnyRole(Object loginId, String... roleIdentifiers) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && hasAnyRole(roleIdentifiers);
    }

    @Override
    public boolean hasAllRole(String... roleIdentifiers) {
        return Arrays.stream(roleIdentifiers).allMatch(this::hasRole);
    }

    @Override
    public boolean hasAllRole(Object loginId, String... roleIdentifiers) {
        return Objects.nonNull(getPrincipalByLoginId(loginId)) && hasAllRole(roleIdentifiers);
    }

    @Override public boolean isAuthenticated() { return true; }
    @Override public boolean isAuthenticated(Object loginId) { return Objects.nonNull(getPrincipalByLoginId(loginId)); }
    @Override public boolean isRemembered() { return false; }
    @Override public boolean isTrustDeviceId(String deviceId) { return false; }
    @Override public boolean isTrustDeviceId(Object userId, String deviceId) { return false; }
    @Override public String login(AuthRequest request) { throw readOnly(); }
    @Override public void logout() { throw readOnly(); }
    @Override public void logout(Object loginId) { throw readOnly(); }
    @Override public void kickout(Object loginId) { throw readOnly(); }
    @Override public String refresh() { throw readOnly(); }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T verify(String token) {
        return (T) verifier.verify(token);
    }

    @Override public void disable(Object loginId, long timeout) { throw readOnly(); }
    @Override public boolean isDisabled(Object loginId) { return false; }
    @Override public void untieDisable(Object loginId) { throw readOnly(); }

    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("OIDC resource-server subject is read-only");
    }

}