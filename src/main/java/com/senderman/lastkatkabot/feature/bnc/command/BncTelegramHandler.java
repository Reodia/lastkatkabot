package com.senderman.lastkatkabot.feature.bnc.command;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.annimon.tgbotsmodule.commands.RegexCommand;
import com.annimon.tgbotsmodule.commands.context.RegexMessageContext;
import com.annimon.tgbotsmodule.services.CommonAbsSender;
import com.senderman.lastkatkabot.Role;
import com.senderman.lastkatkabot.feature.bnc.BncGame;
import com.senderman.lastkatkabot.feature.bnc.exception.*;
import com.senderman.lastkatkabot.feature.bnc.model.BncGameMessage;
import com.senderman.lastkatkabot.feature.bnc.model.BncGameState;
import com.senderman.lastkatkabot.feature.bnc.model.BncResult;
import com.senderman.lastkatkabot.feature.bnc.service.BncGameMessageService;
import com.senderman.lastkatkabot.feature.bnc.service.BncGamesManager;
import com.senderman.lastkatkabot.feature.l10n.context.L10nMessageContext;
import com.senderman.lastkatkabot.feature.l10n.service.L10nService;
import com.senderman.lastkatkabot.feature.userstats.service.UserStatsService;
import com.senderman.lastkatkabot.util.Html;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class BncTelegramHandler implements RegexCommand {

    private final Pattern pattern = Pattern.compile("(\\d|[a-fA-F]){4,16}");
    private final BncGamesManager gamesManager;
    private final UserStatsService usersRepo;
    private final BncGameMessageService gameMessageService;
    private final L10nService localizationService;

    public BncTelegramHandler(
            @Named("bncDatabaseManager") BncGamesManager gamesManager,
            UserStatsService usersRepo,
            BncGameMessageService gameMessageService, L10nService localizationService) {
        this.gamesManager = gamesManager;
        this.usersRepo = usersRepo;
        this.gameMessageService = gameMessageService;
        this.localizationService = localizationService;
    }

    @Override
    public Pattern pattern() {
        return pattern;
    }

    @Override
    public EnumSet<Role> authority() {
        return EnumSet.of(Role.USER);
    }

    public void accept(RegexMessageContext ctx) {
        var l10Ctx = new L10nMessageContext(ctx.sender, ctx.update(), ctx.argumentsAsString(), localizationService);
        var message = ctx.message();
        var telegram = ctx.sender;
        long chatId = ctx.chatId();
        var number = message.getText().toUpperCase(Locale.ENGLISH);
        try {
            var result = gamesManager.check(chatId, number);
            addMessageToDelete(message);
            if (result.isWin()) {
                processWin(gamesManager.getGameState(chatId), l10Ctx, result);
            } else {
                sendGameMessage(ctx.chatId(), formatResult(result, l10Ctx), ctx.sender);
            }
        } catch (NumberAlreadyCheckedException e) {
            addMessageToDelete(message);
            sendGameMessage(
                    chatId,
                    l10Ctx.getString("bnc.handler.alreadyChecked").formatted(formatResult(e.getResult(), l10Ctx)),
                    telegram
            );
        } catch (RepeatingDigitsException e) {
            addMessageToDelete(message);
            sendGameMessage(chatId, l10Ctx.getString("bnc.handler.noRepeatingDigits"), telegram);
        } catch (GameOverException e) {
            addMessageToDelete(message);
            processGameOver(l10Ctx);
        } catch (InvalidLengthException | InvalidCharacterException | NoSuchElementException ignored) {
        }
    }

    public BncGameState getGameState(long chatId) {
        return gamesManager.getGameState(chatId);
    }

    public boolean hasGame(long chatId) {
        return gamesManager.hasGame(chatId);
    }

    public boolean createGameIfNotExists(long chatId, long userId, int length, boolean isHexadecimal) {
        return gamesManager.createGameIfNotExists(chatId, userId, length, isHexadecimal);
    }

    // Send message that will be deleted after game end
    public void sendGameMessage(long chatId, String text, CommonAbsSender sender) {
        var sentMessage = Methods.sendMessage(chatId, text).call(sender);
        if (sentMessage != null)
            addMessageToDelete(sentMessage);
    }

    private void addMessageToDelete(Message message) {
        var chatId = message.getChatId();
        var messageId = message.getMessageId();
        gameMessageService.save(new BncGameMessage(chatId, messageId));
    }

    private void deleteGameMessages(long chatId, CommonAbsSender telegram) {
        var gameMessages = gameMessageService.findByGameId(chatId);
        if (gameMessages.isEmpty()) return;

        gameMessages.stream()
                .map(BncGameMessage::getMessageId)
                .forEach(msgId -> Methods.deleteMessage(chatId, msgId).callAsync(telegram));
        gameMessageService.deleteByGameId(chatId);
    }

    public void processWin(BncGameState game, L10nMessageContext ctx, BncResult result) {
        long chatId = ctx.chatId();
        var userId = ctx.user().getId();
        var userStats = usersRepo.findById(userId);
        int score = game.isHexadecimal() ? (int) (game.length() * 1.5) : game.length();
        userStats.increaseBncScore(score);
        usersRepo.save(userStats);

        var gameState = gamesManager.getGameState(chatId);
        gamesManager.deleteGame(chatId);

        var username = Html.htmlSafe(ctx.user().getFirstName());
        var text = ctx.getString("bnc.handler.userWon").formatted(
                username,
                BncGame.totalAttempts(gameState.length(), gameState.isHexadecimal()) - result.attempts(),
                score,
                formattedHistoryAndTime(gameState, ctx)
        );
        deleteGameMessages(chatId, ctx.sender);
        ctx.reply(text).callAsync(ctx.sender);
    }

    public void processGameOver(L10nMessageContext ctx) {
        long chatId = ctx.chatId();
        var gameState = gamesManager.getGameState(chatId);
        gamesManager.deleteGame(chatId);
        deleteGameMessages(chatId, ctx.sender);
        var text = ctx.getString("bnc.handler.gameOver")
                .formatted(gameState.answer(), formattedHistoryAndTime(gameState, ctx));
        ctx.reply(text).callAsync(ctx.sender);
    }

    public void forceFinishGame(L10nMessageContext ctx, long chatId) {
        if (!gamesManager.hasGame(chatId))
            return;

        var gameState = gamesManager.getGameState(chatId);
        gamesManager.deleteGame(chatId);
        deleteGameMessages(chatId, ctx.sender);

        var text = ctx.getString("bnc.handler.forceFinish")
                .formatted(gameState.answer(), formattedHistoryAndTime(gameState, ctx));
        Methods.sendMessage(chatId, text).callAsync(ctx.sender);
    }

    private String formattedHistoryAndTime(BncGameState state, L10nMessageContext ctx) {
        return ctx.getString("bnc.handler.gameStateStats")
                .formatted(
                        formatHistory(state.history(), ctx),
                        formatTimeSpent((System.currentTimeMillis() - state.startTime()) / 1000)
                );
    }

    private String formatHistory(List<BncResult> history, L10nMessageContext ctx) {
        return history.stream()
                .map(e -> ctx.getString("bnc.handler.historyLine").formatted(e.number(), e.bulls(), e.cows()))
                .collect(Collectors.joining("\n"));
    }

    private String formatTimeSpent(long timeSpent) {
        var sec = timeSpent;
        var mins = sec / 60;
        sec -= mins * 60;
        var hours = mins / 60;
        mins -= hours * 60;
        return "%02d:%02d:%02d".formatted(hours, mins, sec);
    }

    private String formatResult(BncResult result, L10nMessageContext ctx) {
        return ctx.getString("bnc.handler.result").formatted(
                result.number(),
                result.bulls(),
                result.cows(),
                result.attempts()
        );
    }
}
