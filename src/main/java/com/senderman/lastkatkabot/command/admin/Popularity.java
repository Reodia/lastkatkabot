package com.senderman.lastkatkabot.command.admin;

import com.annimon.tgbotsmodule.commands.context.MessageContext;
import com.senderman.lastkatkabot.Role;
import com.senderman.lastkatkabot.command.CommandExecutor;
import com.senderman.lastkatkabot.dbservice.ChatUserService;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class Popularity implements CommandExecutor {

    private final ChatUserService chatUsers;

    public Popularity(ChatUserService chatUsers) {
        this.chatUsers = chatUsers;
    }

    @Override
    public String getTrigger() {
        return "/popularity";
    }

    @Override
    public String getDescription() {
        return "популярность бота";
    }

    @Override
    public EnumSet<Role> getRoles() {
        return EnumSet.of(Role.MAIN_ADMIN, Role.ADMIN);
    }

    @Override
    public void execute(MessageContext ctx) {
        var text = "📊 <b>Популярность бота:</b>\n\n";
        var chatsWithUsers = chatUsers.getTotalChats();
        text += "👥 Активные чаты: " + chatsWithUsers + "\n\n";
        var users = chatUsers.getTotalUsers();
        text += "👤 Уникальные пользователи: " + users;
        ctx.reply(text).callAsync(ctx.sender);
    }
}
