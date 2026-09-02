package io.ddd4j.mq.nats;

import io.ddd4j.mq.MQProperties;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * NATS 客户端配置（纯 Java，零 Spring 依赖）。
 *
 * <p>对应 {@code io.nats:jnats} 原生 NATS / JetStream 客户端。
 * 连接/认证/命名空间等通用字段继承自 {@link MQProperties}，本类仅保留 broker 专属字段。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class NatsProperties extends MQProperties {

    /**
     * NATS 服务器地址列表（逗号分隔，如 {@code nats://host1:4222,nats://host2:4222}）
     */
    private String servers = "nats://localhost:4222";
    /**
     * 客户端名称
     */
    private String connectionName = "ddd4j-nats";
    /**
     * 连接超时（毫秒）
     */
    private long connectTimeoutMillis = 2000L;

    /**
     * 创建并打开 NATS {@link Connection}。
     */
    public Connection connect() {
        try {
            Options.Builder builder = new Options.Builder()
                    .servers(Objects.requireNonNull(servers, "servers").split(","))
                    .connectionName(connectionName)
                    .connectionTimeout(connectTimeoutMillis);
            if (Objects.nonNull(getUsername()) && !io.ddd4j.kit.lang.StrKit.isBlank(getUsername())) {
                builder.userInfo(getUsername(), getPassword());
            }
            return Nats.connect(builder.build());
        } catch (Exception ex) {
            throw new IllegalStateException("Open NATS connection failed", ex);
        }
    }
}
