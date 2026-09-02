package io.ddd4j.core.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 分页数据对象
 * 实现集合接口，集合操作的是records对象
 *
 * @param <T>
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Page<T> implements Iterable<T> {
    // 列表数据
    private List<T> records;
    // 总记录数
    private long total;
    // 回写当前页
    private long current = 1L;
    // 回写每页大小
    private long size = 10L;
    // 扩展字段
    private Map<String, Object> extras;

    /**
     * 构造分页对象（仅指定页码和每页大小）。
     *
     * @param current 当前页码
     * @param size    每页大小
     */
    public Page(long current, long size) {
        this.current = current;
        this.size = size;
        this.total = 0L;
        this.records = new ArrayList<>();
    }

    /**
     * 创建成功的分页结果。
     *
     * @param records 当前页数据列表
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页大小
     * @param <T>     数据类型
     * @return 分页对象
     */
    public static <T> Page<T> succeed(List<T> records, long total, long current, long size) {
        return new Page<>(records, total, current, size, new HashMap<>());
    }

    /**
     * 创建空分页结果。
     *
     * @param <T> 数据类型
     * @return 空分页对象
     */
    public static <T> Page<T> empty() {
        return new Page<>(new ArrayList<>(), 0L, 0L, 0L, Collections.emptyMap());
    }

    @Override
    public Iterator<T> iterator() {
        return Objects.nonNull(this.records) && !this.records.isEmpty() ? this.records.iterator() : null;
    }

    @Override
    public Spliterator<T> spliterator() {
        return Objects.nonNull(this.records) ? this.records.spliterator() : null;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        if (Objects.nonNull(this.records)) {
            this.records.forEach(action);
        }
    }

    /**
     * 判断当前页是否有数据。
     *
     * @return true 表示当前页无数据
     */
    @JsonIgnore
    public boolean isEmpty() {
        return Objects.isNull(this.records) || this.records.isEmpty();
    }

    /**
     * 判断当前页是否包含指定元素。
     *
     * @param o 待检查元素
     * @return true 表示包含
     */
    public boolean contains(Object o) {
        return Objects.nonNull(o) && Objects.nonNull(this.records) && this.records.contains(o);
    }

    /**
     * 向当前页添加元素。
     *
     * @param t 待添加元素
     * @return true 表示添加成功
     */
    public boolean add(T t) {
        return Objects.nonNull(this.records) && this.records.add(t);
    }

    /**
     * 从当前页移除指定元素。
     *
     * @param o 待移除元素
     * @return true 表示移除成功
     */
    public boolean remove(Object o) {
        return Objects.nonNull(this.records) && this.records.remove(o);
    }

    /**
     * 判断当前页是否包含指定集合中的所有元素。
     */
    public boolean containsAll(Collection<?> c) {
        return Objects.nonNull(this.records) && this.records.containsAll(c);
    }

    /**
     * 向当前页添加指定集合中的所有元素。
     */
    public boolean addAll(Collection<? extends T> c) {
        return Objects.nonNull(this.records) && this.records.addAll(c);
    }

    /**
     * 从当前页移除指定集合中的所有元素。
     */
    public boolean removeAll(Collection<?> c) {
        return Objects.nonNull(this.records) && this.records.removeAll(c);
    }

    /**
     * 按条件移除当前页中的元素。
     */
    public boolean removeIf(Predicate<? super T> filter) {
        return Objects.nonNull(this.records) && this.records.removeIf(filter);
    }

    /**
     * 仅保留当前页中包含在指定集合中的元素。
     */
    public boolean retainAll(Collection<?> c) {
        return Objects.nonNull(this.records) && this.records.retainAll(c);
    }

    /**
     * 获取当前页数据的流。
     */
    public Stream<T> stream() {
        return Objects.nonNull(this.records) ? this.records.stream() : new ArrayList<T>().stream();
    }

    public Page<T> peek(Consumer<? super T> action) {
        if (Objects.nonNull(this.records)) {
            this.records.forEach(action);
        }
        return this;
    }

    public Page<T> setRecords(List<T> records) {
        this.records = records;
        return this;
    }

    public Page<T> setTotal(long total) {
        this.total = total;
        return this;
    }

    public Page<T> setSize(long size) {
        this.size = size;
        return this;
    }

    public Page<T> setCurrent(long current) {
        this.current = current;
        return this;
    }

    public Map<String, Object> extras() {
        if (Objects.isNull(extras)) {
            extras = new HashMap<>();
        }
        return extras;
    }
}