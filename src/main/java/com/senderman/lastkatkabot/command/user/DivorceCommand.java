package com.senderman.lastkatkabot.command.user;

import com.annimon.tgbotsmodule.commands.context.MessageContext;
import com.senderman.lastkatkabot.annotation.Command;
import com.senderman.lastkatkabot.callback.Callbacks;
import com.senderman.lastkatkabot.command.CommandExecutor;
import com.senderman.lastkatkabot.dbservice.UserStatsService;
import com.senderman.lastkatkabot.util.callback.ButtonBuilder;
import jakarta.inject.Singleton;

@Singleton
@Command
public class DivorceCommand implements CommandExecutor {

    private final UserStatsService users;

    public DivorceCommand(UserStatsService users) {
        this.users = users;
    }

    @Override
    public String command() {
        return "/divorce";
    }

    @Override
    public String getDescription() {
        return "подать на развод";
    }

    @Override
    public void accept(MessageContext ctx) {

        var userId = ctx.user().getId();
        var userStats = users.findById(userId);
        var loverId = userStats.getLoverId();

        if (loverId == null) {
            ctx.replyToMessage("У вас и так никого нет!").callAsync(ctx.sender);
            return;
        }

        ctx.replyToMessage("Вы точно уверены, что хотите развестись со своей половинкой?")
                .setSingleRowInlineKeyboard(
                        ButtonBuilder.callbackButton()
                                .text("Развестись")
                                .payload(Callbacks.DIVORCE, "a", userId, loverId)
                                .create(),
                        ButtonBuilder.callbackButton()
                                .text("Отмена")
                                .payload(Callbacks.DIVORCE, "d", userId)
                                .create()
                )
                .callAsync(ctx.sender);

    }
}
