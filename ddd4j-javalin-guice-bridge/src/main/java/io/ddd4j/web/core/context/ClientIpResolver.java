package io.ddd4j.web.core.context;

/**
 * 从代理头和远端地址中解析客户端地址。
 */
@FunctionalInterface
public interface ClientIpResolver {

    String resolve(String forwardedFor, String realIp, String remoteAddress);

    static ClientIpResolver remoteAddressOnly() {
        return new DefaultClientIpResolver(false);
    }

    static ClientIpResolver trustedProxy() {
        return new DefaultClientIpResolver(true);
    }
}
