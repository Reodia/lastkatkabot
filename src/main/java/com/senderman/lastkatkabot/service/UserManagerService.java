package com.senderman.lastkatkabot.service;

import com.senderman.lastkatkabot.model.Entity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserManagerService<TUserEntity extends Entity<Integer>> {

    private final CrudRepository<TUserEntity, Integer> repository;
    private final Set<Integer> userIds;

    public UserManagerService(CrudRepository<TUserEntity, Integer> repository) {
        this.repository = repository;
        this.userIds = StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(Entity::getId)
                .collect(Collectors.toSet());
    }

    public boolean hasUser(int id) {
        return userIds.contains(id);
    }

    public boolean addUser(TUserEntity entity) {
        if (userIds.contains(entity.getId())) return false;
        userIds.add(entity.getId());
        repository.save(entity);
        return true;
    }

    public boolean deleteUser(TUserEntity entity) {
        if (!userIds.contains(entity.getId())) return false;
        userIds.remove(entity.getId());
        repository.deleteById(entity.getId());
        return true;
    }


}
