package io.ddd4j.javalin.sample.mybatistc;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * Sample query placeholder. In a real domain this would extend ddd4j-core's
 * {@code Query<T>} contract; the sample keeps it intentionally trivial.
 *
 * @param username 用户名
 */
public final class SampleUserQuery {

    private static final long serialVersionUID = 0L;

    private final String username;

    @JsonCreator()
    public SampleUserQuery(@JsonProperty("username") String username) {
        this.username = username;
    }

    @JsonProperty("username")
    public String username() {
        return username;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        SampleUserQuery other = (SampleUserQuery) obj;
        return Objects.equals(this.username, other.username);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(username);
        return result;
    }

    @Override
    public String toString() {
        return "SampleUserQuery[username=" + username + "]";
    }
}