package io.ddd4j.mq.redisstream;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

import java.util.Map;

/**
 * 跨 Jedis、Redisson 和 Lettuce 的统一 Redis Stream 记录模型。
 *
 * 组件 stream：所属 Stream 名称
 * 组件 id：消息条目 ID
 * 组件 fields：消息字段
 * 组件 nativeMessage：底层原生消息对象
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class RedisStreamRecord {

    private static final long serialVersionUID = 0L;

    private final String stream;

    private final String id;

    private final Map<String, String> fields;

    private final Object nativeMessage;

    @JsonCreator()
    public RedisStreamRecord(@JsonProperty("stream") String stream, @JsonProperty("id") String id, @JsonProperty("fields") Map<String, String> fields, @JsonProperty("nativeMessage") Object nativeMessage) {
        this.stream = stream;
        this.id = id;
        this.fields = fields;
        this.nativeMessage = nativeMessage;
    }

    @JsonProperty("stream")
    public String stream() {
        return stream;
    }

    @JsonProperty("id")
    public String id() {
        return id;
    }

    @JsonProperty("fields")
    public Map<String, String> fields() {
        return fields;
    }

    @JsonProperty("nativeMessage")
    public Object nativeMessage() {
        return nativeMessage;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        RedisStreamRecord other = (RedisStreamRecord) obj;
        return Objects.equals(this.stream, other.stream) && Objects.equals(this.id, other.id) && Objects.equals(this.fields, other.fields) && Objects.equals(this.nativeMessage, other.nativeMessage);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(stream);
        result = 31 * result + Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(fields);
        result = 31 * result + Objects.hashCode(nativeMessage);
        return result;
    }

    @Override
    public String toString() {
        return "RedisStreamRecord[stream=" + stream + ", id=" + id + ", fields=" + fields + ", nativeMessage=" + nativeMessage + "]";
    }
}
