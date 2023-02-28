package com.senderman.lastkatkabot.command.user;

import com.annimon.tgbotsmodule.commands.context.MessageContext;
import com.senderman.lastkatkabot.annotation.Command;
import com.senderman.lastkatkabot.callback.Callbacks;
import com.senderman.lastkatkabot.command.CommandExecutor;
import com.senderman.lastkatkabot.util.Html;
import com.senderman.lastkatkabot.util.callback.ButtonBuilder;
import jakarta.inject.Singleton;

@Singleton
@Command
public class StartDuelCommand implements CommandExecutor {

    @Override
    public String command() {
        return "/duel";
    }

    @Override
    public String getDescription() {
        return "начать дуэль";
    }

    @Override
    public void accept(MessageContext ctx) {
        var user = ctx.user();
        var name = Html.htmlSafe(user.getFirstName());
        ctx.reply("🎯 Пользователь " + name + " начинает набор на дуэль!")
                .setInlineKeyboard(ButtonBuilder.callbackButton()
                        .text("Присоединиться")
                        .payload(Callbacks.DUEL, user.getId())
                        .create())
                .callAsync(ctx.sender);
    }
}
