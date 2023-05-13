package com.senderman.lastkatkabot.feature.tracking.command;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.annimon.tgbotsmodule.services.CommonAbsSender;
import com.senderman.lastkatkabot.Role;
import com.senderman.lastkatkabot.command.Command;
import com.senderman.lastkatkabot.command.CommandExecutor;
import com.senderman.lastkatkabot.feature.localization.context.LocalizedMessageContext;
import com.senderman.lastkatkabot.feature.tracking.model.ChatUser;
import com.senderman.lastkatkabot.feature.tracking.service.ChatUserService;
import com.senderman.lastkatkabot.util.Html;
import jakarta.inject.Named;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;

@Command
public class WhoInChatCommand implements CommandExecutor {

    private final ChatUserService chatUsers;
    private final ExecutorService threadPool;

    public WhoInChatCommand(ChatUserService chatUsers, @Named("generalNeedsPool") ExecutorService threadPool) {
        this.chatUsers = chatUsers;
        this.threadPool = threadPool;
    }

    @Override
    public String command() {
        return "/wic";
    }

    @Override
    public String getDescription() {
        return "tracking.wic.description";
    }

    @Override
    public EnumSet<Role> authority() {
        return EnumSet.of(Role.ADMIN, Role.MAIN_ADMIN);
    }

    @Override
    public void accept(@NotNull LocalizedMessageContext ctx) {
        if (ctx.argumentsLength() < 1) {
            ctx.replyToMessage(ctx.getString("tracking.wic.wrongUsage")).callAsync(ctx.sender);
            return;
        }
        long chatId;
        try {
            chatId = Long.parseLong(ctx.argument(0));
        } catch (NumberFormatException e) {
            ctx.replyToMessage(ctx.getString("common.chatIdIsNumber")).callAsync(ctx.sender);
            return;
        }

        threadPool.execute(() -> {
            var users = chatUsers.findByChatId(chatId)
                    .stream()
                    .map(this::formatUser)
                    .toList();

            if (users.isEmpty()) {
                ctx.replyToMessage(ctx.getString("tracking.wic.chatIsEmpty")).callAsync(ctx.sender);
                return;
            }

            var text = new StringBuilder(ctx.getString("tracking.wic.usersFound")
                    .formatted(getChatNameOrChatId(chatId, ctx.sender)));
            for (var user : users) {
                if (text.length() + "\n".length() + user.length() >= 4096) {
                    ctx.replyToMessage(text.toString()).callAsync(ctx.sender);
                    text.setLength(0);
                }
                text.append(user).append("\n");
            }
            // send remaining users
            if (text.length() != 0) {
                ctx.replyToMessage(text.toString()).callAsync(ctx.sender);
            }
        });

    }

    private String formatUser(ChatUser user) {
        return "%s (<code>%d</code>)".formatted(Html.getUserLink(user.getUserId(), user.getName()), user.getUserId());
    }

    // get chat name. If unable to get if from tg, return chatId as string
    private String getChatNameOrChatId(long chatId, CommonAbsSender telegram) {
        var chat = Methods.getChat(chatId).call(telegram);
        return chat != null ? Html.htmlSafe(chat.getTitle()) + " (<code>%d</code>)".formatted(chatId) : String.valueOf(chatId);
    }
}
