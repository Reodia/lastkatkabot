package com.senderman.lastkatkabot.command.user;

import com.annimon.tgbotsmodule.commands.context.MessageContext;
import com.senderman.lastkatkabot.annotation.Command;
import com.senderman.lastkatkabot.command.CommandExecutor;

@Command(
        command = "/shortinfo",
        description = "краткая инфа о сообщении. Поддерживается реплай"
)
public class ShortInfoCommand extends CommandExecutor {

    @Override
    public void accept(MessageContext ctx) {
        var chatId = ctx.chatId();
        var userId = ctx.user().getId();

        String info = String.format("""
                ==== Информация ====

                💬 ID чата: <code>%d</code>
                🙍‍♂️ Ваш ID: <code>%d</code>""", chatId, userId);

        var message = ctx.message();
        if (message.isReply()) {
            var reply = message.getReplyToMessage();
            var replyMessageId = reply.getMessageId();
            var replyUserId = reply.getFrom().getId();
            info += String.format("""


                    ✉️ ID reply: <code>%d</code>
                    🙍‍♂ ID юзера из reply: <code>%d</code>""", replyMessageId, replyUserId);

            var forward = reply.getForwardFromChat();
            if (forward != null && forward.isChannelChat()) {
                info += String.format("\n\uD83D\uDCE2 ID канала: <code>%d</code>", forward.getId());
            }
        }
        ctx.reply(info).callAsync(ctx.sender);

    }
}
