package io.ddd4j.core.api;

import io.ddd4j.core.contract.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 统一接口响应，标准的响应数据结构
 *
 * @param <T>
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@AllArgsConstructor
public class R<T> implements IR {

    // 编码：0/200、请求成功；500、请求成功但服务异常；403、未登录或者token已失效；401、已登录没有权限。
    protected Serializable code;
    // 返回信息
    protected String msg;
    // 响应数据
    protected T data;

    public R() {
        this(ResultCode.OK.getCode(), ResultCode.OK.getDesc());
    }

    public R(Serializable code, String msg) {
        this(code, msg, null);
    }

    public R(T data) {
        this(ResultCode.OK.getCode(), ResultCode.OK.getDesc(), data);
    }

    public static <T> R<T> ok() {
        return new R<>();
    }

    public static <T> R<T> ok(T payload) {
        return new R<>(payload);
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R(ResultCode.OK.getCode(), msg, data);
    }

    public static <T> R<T> fail(Serializable code, String msg) {
        return new R(code, msg);
    }

    public static <T> R<T> fail(Serializable code, String msg, T data) {
        return new R(code, msg, data);
    }

    public static <T> R<T> fail() {
        return fail(ResultCode.FAIL.getCode());
    }

    public static <T> R<T> fail(Serializable code) {
        return fail(code, ResultCode.FAIL.getDesc());
    }

    public static <T> R<T> fail(String msg) {
        return fail(ResultCode.FAIL.getCode(), msg);
    }

    // === cloud 兼容别名（failed = fail，isOk 语义对齐 cloud SUCCESS=0） ===

    /**
     * 失败响应（cloud 兼容别名，等价于 {@link #fail()}）。
     */
    public static <T> R<T> failed() {
        return fail();
    }

    /**
     * 失败响应（cloud 兼容别名，等价于 {@link #fail(String)}）。
     */
    public static <T> R<T> failed(String msg) {
        return fail(msg);
    }

    /**
     * 失败响应（cloud 兼容别名，等价于 {@link #fail(T)}）。
     */
    public static <T> R<T> failed(T data) {
        return fail(Objects.nonNull(data) ? ResultCode.FAIL.getCode() : ResultCode.FAIL.getCode(), ResultCode.FAIL.getDesc());
    }

    /**
     * 失败响应（cloud 兼容别名，等价于 {@link #fail(Serializable, String)}）。
     */
    public static <T> R<T> failed(Serializable code, String msg) {
        return fail(code, msg);
    }

    /**
     * 失败响应（cloud 兼容别名，等价于 {@link #fail(Serializable, String, T)}）。
     */
    public static <T> R<T> failed(T data, String msg) {
        return fail(ResultCode.FAIL.getCode(), msg, data);
    }

    /**
     * 失败响应（cloud 兼容别名，带 data + code + msg）。
     */
    public static <T> R<T> failed(T data, Serializable code, String msg) {
        return fail(code, msg, data);
    }

    public static boolean empty(R<?> r) {
        return Objects.isNull(r) || !Objects.equals(r.getCode(), ResultCode.OK.getCode()) || Objects.isNull(r.getData());
    }

    public static <T> R<T> transform(R source) {
        R<T> target = new R();
        target.setCode(source.getCode());
        target.setMsg(source.getMsg());
        return target;
    }

    public Boolean isOk() {
        return Objects.equals(this.getCode(), ResultCode.OK.getCode()) || Objects.equals(this.getCode(), ResultCode.SUCCESS.getCode());
    }

    public Boolean isEmpty() {
        return !isOk() || Objects.isNull(data);
    }

}
