package com.senderman.lastkatkabot.feature.access.service;

import com.senderman.lastkatkabot.feature.access.model.UserIdAndName;
import io.micronaut.data.repository.CrudRepository;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class UserManager<TUserEntity extends UserIdAndName<Long>> {

    private final CrudRepository<TUserEntity, Long> repository;
    private final Set<Long> userIds;

    public UserManager(CrudRepository<TUserEntity, Long> repository) {
        this.repository = repository;
        this.userIds = repository.findAll().stream()
                .map(UserIdAndName::getUserId)
                .collect(Collectors.toSet());
    }

    public Iterable<TUserEntity> findAll() {
        return repository.findAll();
    }

    public boolean hasUser(long id) {
        return userIds.contains(id);
    }

    public boolean addUser(TUserEntity entity) {
        if (userIds.contains(entity.getUserId())) return false;
        userIds.add(entity.getUserId());
        repository.update(entity);
        return true;
    }

    public boolean deleteById(long id) {
        if (!userIds.remove(id)) return false;
        repository.deleteById(id);
        return true;
    }


}
