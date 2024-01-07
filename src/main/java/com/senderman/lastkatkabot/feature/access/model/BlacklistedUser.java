package com.senderman.lastkatkabot.feature.access.model;


import io.micronaut.core.annotation.Creator;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.util.Objects;

@MappedEntity("BLACKLISTED_USER")
public class BlacklistedUser implements UserIdAndName<Long> {

    @Id
    @MappedProperty("user_id")
    private final long userId;

    @MappedProperty("name")
    private final String name;

    @Creator
    public BlacklistedUser(@Id long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    @Id
    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlacklistedUser that = (BlacklistedUser) o;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "BlacklistedUser{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public String getName() {
        return name;
    }
}
