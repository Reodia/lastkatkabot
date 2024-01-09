package com.senderman.lastkatkabot.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.senderman.lastkatkabot.feature.genshin.model.Item;
import com.senderman.lastkatkabot.util.ResourceFiles;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.telegram.telegrambots.bots.DefaultBotOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Factory
public class Beans {

    @Singleton
    public DefaultBotOptions botOptions() {
        var options = new DefaultBotOptions();
        options.setAllowedUpdates(List.of("message", "callback_query"));
        return options;
    }

    @Singleton
    @Named("love")
    public Map<String, List<String>> love() throws IOException {
        var loveFiles = ResourceFiles.getResourceFiles("/love");
        Map<String, List<String>> stringMap = new HashMap<>();

        for (String element : loveFiles) {
            List<String> loveStrings = new YAMLMapper()
                    .readValue(getClass().getResourceAsStream("/love/" + element), new TypeReference<>() {
                    });
            stringMap.put(element.substring(0, element.indexOf('.')), loveStrings);
        }
        return stringMap;
    }

    @Singleton
    @Named("genshinItems")
    public List<Item> genshinItems() throws IOException {
        try (var in = getClass().getResourceAsStream("/genshin/items.yml")) {
            return new YAMLMapper().readValue(in, new TypeReference<>() {
            });
        }
    }

    @Singleton
    @Named("messageToJsonMapper")
    public ObjectMapper messageToJsonMapper() {
        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }
}
