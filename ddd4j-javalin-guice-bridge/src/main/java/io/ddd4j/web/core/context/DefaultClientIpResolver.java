package io.ddd4j.web.core.context;

import io.ddd4j.kit.lang.StrKit;

/**
 * 默认客户端地址解析器。只有显式信任反向代理时才读取转发头。
 */
public final class DefaultClientIpResolver implements ClientIpResolver {

    private final boolean trustForwardedHeaders;

    public DefaultClientIpResolver(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @Override
    public String resolve(String forwardedFor, String realIp, String remoteAddress) {
        if (trustForwardedHeaders) {
            String forwardedAddress = firstForwardedAddress(forwardedFor);
            if (StrKit.isNotBlank(forwardedAddress)) {
                return forwardedAddress;
            }
            if (StrKit.isNotBlank(realIp)) {
                return realIp.trim();
            }
        }
        return StrKit.isBlank(remoteAddress) ? null : remoteAddress.trim();
    }

    private String firstForwardedAddress(String forwardedFor) {
        if (StrKit.isBlank(forwardedFor)) {
            return null;
        }
        int separator = forwardedFor.indexOf(',');
        String address = separator < 0 ? forwardedFor : forwardedFor.substring(0, separator);
        return StrKit.isBlank(address) ? null : address.trim();
    }
}
