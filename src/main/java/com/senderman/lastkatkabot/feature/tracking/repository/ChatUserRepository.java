package com.senderman.lastkatkabot.feature.tracking.repository;

import com.senderman.lastkatkabot.feature.tracking.model.ChatUser;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Sort;
import io.micronaut.data.mongodb.annotation.MongoAggregateQuery;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.mongodb.annotation.MongoUpdateOptions;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@MongoRepository
public interface ChatUserRepository extends CrudRepository<ChatUser, String> {

    @MongoAggregateQuery("""
            [
            { $match: { chatId: :chatId } },
            { $sample: { size: :amount } }
            ]""")
    List<ChatUser> sampleOfChat(long chatId, int amount);

    List<ChatUser> findByUserId(long userId);

    //@Query(value = "{ userId: ?0 }", sort = "{ lastMessageDate: -1} ")
    Optional<ChatUser> findFirstByUserId(long userId, Sort sort);

    List<ChatUser> findByChatId(long chatId);

    Optional<ChatUser> findByChatIdAndUserId(long chatId, long userId);

    void deleteByChatIdAndLastMessageDateLessThan(long chatId, int lastMessageDate);

    long deleteByLastMessageDateLessThan(int lastMessageDate);

    void deleteByChatIdAndUserId(long chatId, long userId);

    boolean existsByChatIdAndUserId(long chatId, long userId);

    long countByChatId(long chatId);

    @Override
    @MongoUpdateOptions(upsert = true)
    <S extends ChatUser> List<S> updateAll(@NonNull Iterable<S> entities);
}
