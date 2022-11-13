package com.senderman.lastkatkabot.command.user;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.annimon.tgbotsmodule.commands.context.MessageContext;
import com.senderman.lastkatkabot.annotation.Command;
import com.senderman.lastkatkabot.command.CommandExecutor;
import com.senderman.lastkatkabot.dbservice.UserStatsService;
import com.senderman.lastkatkabot.service.weather.Forecast;
import com.senderman.lastkatkabot.service.weather.NoSuchCityException;
import com.senderman.lastkatkabot.service.weather.ParseException;
import com.senderman.lastkatkabot.service.weather.WeatherService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Command(
        command = "/weather",
        description = "погода. Если не указать город, то покажет погоду в последнем введенном вами городе"
)
public class WeatherCommand extends CommandExecutor {

    private final UserStatsService userStats;
    private final WeatherService weatherService;
    private final ExecutorService threadPool;

    public WeatherCommand(
            UserStatsService userStats,
            WeatherService weatherService,
            @Qualifier("generalNeedsPool") ExecutorService threadPool) {
        this.userStats = userStats;
        this.weatherService = weatherService;
        this.threadPool = threadPool;
    }

    @Override
    public void accept(@NotNull MessageContext ctx) {
        final var messageToEdit = ctx.replyToMessage("\uD83C\uDF10 Запрос в очереди...").call(ctx.sender);
        // if bot failed to send the message
        if (messageToEdit == null) {
            return;
        }
        final Consumer<String> editMessageConsumer = s -> Methods.editMessageText(
                messageToEdit.getChatId(),
                messageToEdit.getMessageId(),
                s
        ).callAsync(ctx.sender);

        threadPool.execute(() -> {
            try {
                editMessageConsumer.accept("\uD83C\uDF10 Соединение...");
                String city = getCityFromMessageOrDb(ctx);
                var text = forecastToString(weatherService.getWeatherByCity(city));
                editMessageConsumer.accept(text);
                // save last defined city in db (we won't get here if exception is occurred)
                updateUserCity(ctx.user().getId(), city);
            } catch (NoCitySpecifiedException e) {
                editMessageConsumer.accept("Вы не указали город! (/weather город). Бот запомнит ваш выбор.");
            } catch (NoSuchCityException e) {
                editMessageConsumer.accept("Город не найден - " + e.getCity());
            } catch (ParseException e) {
                editMessageConsumer.accept("Ошибка обработки запроса: " + e.getMessage());
                throw new RuntimeException(e);
            } catch (IOException e) {
                editMessageConsumer.accept("Ошибка соединения с сервисом погоды");
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Get city from message text. If it's empty, query db for it
     *
     * @param ctx message context
     * @return user's city
     * @throws NoCitySpecifiedException if the city is found neither in message text, neither in db
     */
    private String getCityFromMessageOrDb(MessageContext ctx) throws NoCitySpecifiedException {
        ctx.setArgumentsLimit(1);
        if (ctx.argumentsLength() != 0)
            return ctx.argument(0);

        var city = userStats.findById(ctx.user().getId()).getCityLink();
        if (city != null) return city;
        throw new NoCitySpecifiedException();
    }

    private void updateUserCity(long userId, String city) {
        var user = userStats.findById(userId);
        user.setCityLink(city);
        userStats.save(user);
    }


    private String forecastToString(Forecast forecast) {
        return "<b>" + forecast.title() + "</b>\n\n" +
                forecast.feelings() + "\n" +
                "🌡: " + forecast.temperature() + "\n" +
                "🤔: Ощущается как " + forecast.feelsLike() + "\n" +
                "💨: " + forecast.wind() + "\n" +
                "💧: " + forecast.humidity() + "\n" +
                "🧭: " + forecast.pressure();
    }

    private static class NoCitySpecifiedException extends Exception {

    }

}
