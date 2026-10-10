package io.ddd4j.javalin.sample.mybatistc;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * Sample domain object (POJO / record). Kept minimal so the sample focuses on
 * MyBatis-Plus wiring rather than DDD aggregate plumbing.
 */
public final class SampleUser {

    private static final long serialVersionUID = 0L;

    private final Long id;

    private final String username;

    private final String email;

    public static SampleUser of(Long id, String username, String email) {
        return new SampleUser(id, username, email);
    }

    @JsonCreator()
    public SampleUser(@JsonProperty("id") Long id, @JsonProperty("username") String username, @JsonProperty("email") String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    @JsonProperty("id")
    public Long id() {
        return id;
    }

    @JsonProperty("username")
    public String username() {
        return username;
    }

    @JsonProperty("email")
    public String email() {
        return email;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        SampleUser other = (SampleUser) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.username, other.username) && Objects.equals(this.email, other.email);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(username);
        result = 31 * result + Objects.hashCode(email);
        return result;
    }

    @Override
    public String toString() {
        return "SampleUser[id=" + id + ", username=" + username + ", email=" + email + "]";
    }
}