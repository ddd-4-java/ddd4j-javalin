package io.ddd4j.web.core.context;

/**
 * 可由运行时事件总线观测的框架无关 HTTP 请求失败事件。
 */
public record WebRequestFailure(String method, String path, Throwable cause) {
}
