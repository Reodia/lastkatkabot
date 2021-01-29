package com.senderman.lastkatkabot.bnc;

import com.senderman.lastkatkabot.ApiRequests;
import com.senderman.lastkatkabot.model.Userstats;
import com.senderman.lastkatkabot.repository.UserStatsRepository;
import com.senderman.lastkatkabot.util.Html;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Component
public class BncTelegramHandler {

    private final ApiRequests telegram;
    private final BncDatabaseController bncDatabaseController;
    private final UserStatsRepository usersRepo;

    public BncTelegramHandler(
            ApiRequests telegram,
            BncDatabaseController bncDatabaseController,
            UserStatsRepository usersRepo
    ) {
        this.telegram = telegram;
        this.bncDatabaseController = bncDatabaseController;
        this.usersRepo = usersRepo;
    }

    public void processBncAnswer(Message message) {
        var chatId = message.getChatId();
        var number = message.getText();
        try {
            var result = bncDatabaseController.check(chatId, number);
            if (result.isWin()) {
                processWin(message, result);
            } else {
                telegram.sendMessage(chatId, formatResult(result));
            }
        } catch (NumberAlreadyCheckedException e) {
            telegram.sendMessage(chatId, "Уже проверяли! " + formatResult(e.getResult()));
        } catch (GameOverException e) {
            processGameOver(message, e.getAnswer());
        } catch (InvalidLengthException | NoSuchElementException ignored) {

        }
    }

    public void processWin(Message message, BncResult result) {
        var chatId = message.getChatId();
        var userId = message.getFrom().getId();
        var userStats = usersRepo.findById(userId).orElse(new Userstats(userId));
        userStats.increaseBncScore(result.getNumber().length());
        usersRepo.save(userStats);
        var gameState = bncDatabaseController.getGameState(chatId);
        bncDatabaseController.deleteGame(chatId);
        var username = Html.htmlSafe(message.getFrom().getFirstName());
        var text = username + " выиграл за " + (BncGame.totalAttempts(gameState.getLength()) - result.getAttempts()) +
                " попыток!\n\n" + formatGameEndMessage(gameState);
        telegram.sendMessage(chatId, text);
    }

    public void processGameOver(Message message, String answer) {
        var chatId = message.getChatId();
        var gameState = bncDatabaseController.getGameState(chatId);
        bncDatabaseController.deleteGame(chatId);
        var text = "Вы проиграли! Ответ: " + answer + "\n\n" + formatGameEndMessage(gameState);
        telegram.sendMessage(chatId, text);
    }

    private String formatGameEndMessage(BncGameState state) {
        return formatHistory(state.getHistory()) +
                "\n\nПотрачено времени: " +
                formatTimeSpent((System.currentTimeMillis() - state.getStartTime()) / 1000);
    }

    private String formatHistory(List<BncResult> history) {
        return history.stream()
                .map(e -> String.format("%s: %dБ %dК", e.getNumber(), e.getBulls(), e.getCows()))
                .collect(Collectors.joining("\n"));
    }

    private String formatTimeSpent(long timeSpent) {
        var sec = timeSpent;
        var mins = sec / 60;
        sec -= mins * 60;
        var hours = mins / 60;
        mins -= hours * 60;
        return String.format("%02d:%02d:%02d", hours, mins, sec);
    }

    private String formatResult(BncResult result) {
        return String.format("%s: %dБ %dК, попыток: %d", result.getNumber(),
                result.getBulls(),
                result.getCows(),
                result.getAttempts());
    }
}
