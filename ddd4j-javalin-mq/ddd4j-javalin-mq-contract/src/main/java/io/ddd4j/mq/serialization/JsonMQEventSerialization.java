package io.ddd4j.mq.serialization;

import io.ddd4j.kit.lang.JsonKit;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.event.MQEventSerialization;

import java.util.Objects;

/**
 * 默认 JSON 消息序列化实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class JsonMQEventSerialization implements MQEventSerialization {

    @Override
    public <S, T> T deserialize(S src, Class<T> dist) throws RuntimeException {
        if (Objects.isNull(src)) {
            return null;
        }
        String text = src instanceof String s ? s : String.valueOf(src);
        if (StrKit.isEmpty(text)) {
            return null;
        }
        return JsonKit.toObject(text, dist);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T serialize(Object src) throws RuntimeException {
        return (T) JsonKit.toJson(src);
    }
}
