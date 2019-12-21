package com.senderman.lastkatkabot.usercommands

import com.annimon.tgbotsmodule.api.methods.Methods
import com.senderman.CommandExecutor
import com.senderman.lastkatkabot.LastkatkaBotHandler
import org.telegram.telegrambots.meta.api.objects.Message

class GetRow constructor(val handler: LastkatkaBotHandler) : CommandExecutor {
    override val command: String
        get() = "/getrow"
    override val desc: String
        get() = "показать сообщение с рассчетом юзеров"

    override fun execute(message: Message) {
        if (!message.isGroupMessage && !message.isSuperGroupMessage) return
        if (message.chatId !in handler.userRows)
            handler.sendMessage(message.chatId, "У вас пока еще нет списка!")
        else
            handler.sendMessage(Methods.sendMessage(message.chatId, "Вот!")
                    .setReplyToMessageId(handler.userRows[message.chatId]?.messageId))
    }
}