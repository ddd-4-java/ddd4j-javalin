package io.ddd4j.javalin.auth.license;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for ddd4j-javalin-auth-license, bound from
 * {@code ddd4j.auth.license.*} keys.
 *
 * <p>Mirrors {@code io.ddd4j.auth.license.LicenseProperties} but expressed as a
 * plain POJO so the Javalin module doesn't need to depend on the ddd4j extension
 * module's Lombok-generated setters.
 */
@Getter
@Setter
public class LicenseProperties {

    public static final String PREFIX = "ddd4j.auth.license";

    /** License certificate subject. */
    private String subject;

    /** Public-key alias inside the keystore. */
    private String publicAlias;

    /** Password protecting the keystore. */
    private String storePass;

    /** Path to the {@code .lic} certificate file. */
    private String licensePath;

    /** Path to the {@code publicKeys.store} keystore. */
    private String publicKeysStorePath;

    /** Whether to install the certificate on startup. Default {@code true}. */
    private boolean installOnStart = true;
}