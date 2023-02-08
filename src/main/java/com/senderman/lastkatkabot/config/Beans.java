package com.senderman.lastkatkabot.config;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.senderman.lastkatkabot.BotHandler;
import com.senderman.lastkatkabot.genshin.Item;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

@Configuration
public class Beans {

    @Bean
    public DefaultBotOptions botOptions() {
        var options = new DefaultBotOptions();
        options.setAllowedUpdates(List.of("message", "callback_query"));
        return options;
    }

    @Bean
    public List<String> love() throws IOException {
        return new YAMLMapper().readValue(getClass().getResourceAsStream("/love.yml"), new TypeReference<>() {
        });
    }

    @Bean
    public List<Item> genshinItems() throws IOException {
        return new YAMLMapper().readValue(getClass().getResourceAsStream("/genshin/items.yml"), new TypeReference<>() {
        });
    }

    @Bean
    public Consumer<Long> chatPolicyViolationConsumer(BotHandler handler) {
        return (chatId) -> {
            Methods.sendMessage(chatId, "📛 Ваш чат в списке спамеров! Бот не хочет здесь работать!").callAsync(handler);
            Methods.leaveChat(chatId).callAsync(handler);
        };
    }

    @Bean
    public ObjectMapper messageToJsonMapper() {
        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

}
