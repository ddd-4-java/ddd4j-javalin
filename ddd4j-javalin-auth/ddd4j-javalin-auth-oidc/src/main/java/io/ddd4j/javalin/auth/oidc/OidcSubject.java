package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.subject.Subject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** OIDC JWT 的只读 Subject 适配器；会话变更由外部身份提供方负责。 */
final class OidcSubject implements Subject {

    private final ThreadLocal<AuthPrincipal> current;
    private final OidcTokenVerifier verifier;

    OidcSubject(ThreadLocal<AuthPrincipal> current, OidcTokenVerifier verifier) {
        this.current = current;
        this.verifier = verifier;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T getPrincipal() {
        return (T) current.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T getPrincipalByLoginId(Object loginId) {
        AuthPrincipal principal = current.get();
        return Objects.nonNull(principal) && Objects.equals(loginId, principal.getLoginId()) ? (T) principal : null;
    }

    @Override
    public <T extends AuthPrincipal> T getPrincipalByToken(String tokenValue) {
        return verify(tokenValue);
    }

    @Override
    public boolean isPermitted(String permission) {
        AuthPrincipal principal = current.get();
        return Objects.nonNull(principal) && safePermissions(principal).contains(permission);
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
        AuthPrincipal principal = current.get();
        return Objects.nonNull(principal) && safeRoles(principal).stream().anyMatch(role -> roleIdentifier.equals(role.getRoleCode()) || roleIdentifier.equals(role.getRoleId()));
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

    @Override public boolean isAuthenticated() { return Objects.nonNull(current.get()); }
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

    private Set<String> safePermissions(AuthPrincipal principal) {
        return Objects.isNull(principal.getPerms()) ? Collections.emptySet() : principal.getPerms();
    }

    private List<AuthPrincipal.RolePair> safeRoles(AuthPrincipal principal) {
        return Objects.isNull(principal.getRoles()) ? Collections.emptyList() : principal.getRoles();
    }

    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("OIDC resource-server subject is read-only");
    }

}
