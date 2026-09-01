package io.ddd4j.javalin.data.panache;

import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RepositoryUtil} 纯单元测试：不依赖容器 / ORM 运行时，直接断言
 * 过滤 Map → HQL 片段 + 命名参数、排序列表 → Panache {@link Sort} 的映射规则。
 *
 * <p>注意：{@link RepositoryUtil#formQuery} 按入参 Map 迭代顺序生成片段并按下标编号
 * （{@code key + seq}），测试统一使用 {@link LinkedHashMap} 保证顺序确定。
 */
class RepositoryUtilTest {

    @Test
    void shouldBuildEqualityWhereClause() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("name", "alice");
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(filters, params);

        assertThat(where).isEqualTo("name = :name0");
        assertThat(params).containsExactly(Map.entry("name0", "alice"));
    }

    @Test
    void shouldBuildNotInClauseWithBracketPrefix() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("[!]id", List.of(1, 2));
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(filters, params);

        assertThat(where).isEqualTo("id NOT IN :id0");
        assertThat(params).containsExactly(Map.entry("id0", List.of(1, 2)));
    }

    @Test
    void shouldBuildInClauseWithEmptyBracketPrefix() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("[]id", List.of(1, 2));
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(filters, params);

        assertThat(where).isEqualTo("id IN :id0");
        assertThat(params).containsExactly(Map.entry("id0", List.of(1, 2)));
    }

    @Test
    void shouldBuildNotEqualClauseWithBangPrefix() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("!status", "X");
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(filters, params);

        assertThat(where).isEqualTo("status <> :status0");
        assertThat(params).containsExactly(Map.entry("status0", "X"));
    }

    @Test
    void shouldBuildLikeClauseWithPercentPrefix() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("%name", "ab");
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(filters, params);

        assertThat(where).isEqualTo("name like :name0");
        // LIKE 参数值由工具类补上两侧通配符
        assertThat(params).containsExactly(Map.entry("name0", "%ab%"));
    }

    @Test
    void shouldBuildGreaterThanEqualClause() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put(">=age", 18);
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(filters, params);

        assertThat(where).isEqualTo("age >= :age0");
        assertThat(params).containsExactly(Map.entry("age0", 18));
    }

    @Test
    void shouldBuildLessThanClause() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("<age", 65);
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(filters, params);

        assertThat(where).isEqualTo("age < :age0");
        assertThat(params).containsExactly(Map.entry("age0", 65));
    }

    @Test
    void shouldBuildEqualityWithEqualsPrefix() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("=score", 100);
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(filters, params);

        assertThat(where).isEqualTo("score = :score0");
        assertThat(params).containsExactly(Map.entry("score0", 100));
    }

    @Test
    void shouldBuildMultiConditionWithAndSeparator() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("name", "alice");
        filters.put(">=age", 18);
        filters.put("!status", "X");
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(filters, params);

        assertThat(where).isEqualTo("name = :name0 AND age >= :age1 AND status <> :status2");
        assertThat(params).containsExactly(
                Map.entry("name0", "alice"),
                Map.entry("age1", 18),
                Map.entry("status2", "X"));
    }

    @Test
    void shouldReturnEmptyStringWhenFiltersEmpty() {
        Map<String, Object> params = new LinkedHashMap<>();

        String where = RepositoryUtil.formQuery(new LinkedHashMap<>(), params);

        assertThat(where).isEmpty();
        assertThat(params).isEmpty();
    }

    @Test
    void shouldReturnEmptySortWhenSortingEmpty() {
        Sort sort = RepositoryUtil.from(List.of());

        assertThat(sort.getColumns()).isEmpty();
    }

    @Test
    void shouldBuildAscendingSortWithPlusPrefix() {
        Sort sort = RepositoryUtil.from(List.of("+name"));

        assertThat(sort.getColumns()).hasSize(1);
        assertThat(sort.getColumns().get(0).getName()).isEqualTo("name");
        assertThat(sort.getColumns().get(0).getDirection()).isEqualTo(Sort.Direction.Ascending);
    }

    @Test
    void shouldBuildDescendingSortWithMinusPrefix() {
        Sort sort = RepositoryUtil.from(List.of("-createdAt"));

        assertThat(sort.getColumns()).hasSize(1);
        assertThat(sort.getColumns().get(0).getName()).isEqualTo("createdAt");
        assertThat(sort.getColumns().get(0).getDirection()).isEqualTo(Sort.Direction.Descending);
    }

    @Test
    void shouldDefaultToAscendingSortWithoutPrefix() {
        Sort sort = RepositoryUtil.from(List.of("-createdAt", "name"));

        assertThat(sort.getColumns()).hasSize(2);
        assertThat(sort.getColumns().get(1).getName()).isEqualTo("name");
        assertThat(sort.getColumns().get(1).getDirection()).isEqualTo(Sort.Direction.Ascending);
    }
}
