package com.senderman.lastkatkabot.handlers

import com.annimon.tgbotsmodule.api.methods.Methods
import com.senderman.Command
import com.senderman.TgUser
import com.senderman.lastkatkabot.LastkatkaBot
import com.senderman.lastkatkabot.LastkatkaBotHandler
import com.senderman.lastkatkabot.Services
import com.senderman.lastkatkabot.tempobjects.BnCPlayer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.ChatMember
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.logging.BotLogger
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ThreadLocalRandom

class UsercommandsHandler(private val handler: LastkatkaBotHandler) {
    fun action(message: Message) {
        Methods.deleteMessage(message.chatId, message.messageId).call(handler)
        if (message.text.split("\\s+".toRegex()).size == 1) return

        val action = message.text.split("\\s+".toRegex(), 2)[1]
        val sm = Methods.sendMessage(message.chatId, message.from.firstName + " " + action)
        if (message.isReply) sm.replyToMessageId = message.replyToMessage.messageId
        handler.sendMessage(sm)
    }

    fun pressF(message: Message) {
        if (message.isUserMessage) return
        if (message.isReply && message.from.firstName == message.replyToMessage.from.firstName) return

        val `object` = if (message.text.split(" ").size > 1)
            message.text.split(" ".toRegex(), 2)[1]
        else
            message.replyToMessage.from.firstName

        Methods.deleteMessage(message.chatId, message.messageId).call(handler)
        val text = "\uD83D\uDD6F Press F to pay respects to $`object`" +
                "\n${message.from.firstName} has payed respects"
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.chatId)
                .setText(text)
                .setReplyMarkup(markupForPayingRespects))
    }

    fun cake(message: Message) {
        if (message.isUserMessage) return
        val markup = InlineKeyboardMarkup()
        markup.keyboard = listOf(listOf(
                InlineKeyboardButton()
                        .setText("Принять")
                        .setCallbackData(LastkatkaBot.CALLBACK_CAKE_OK + message.text
                                .replace("/cake", "")),
                InlineKeyboardButton()
                        .setText("Отказаться")
                        .setCallbackData(LastkatkaBot.CALLBACK_CAKE_NOT + message.text
                                .replace("/cake", ""))))

        Methods.deleteMessage(message.chatId, message.messageId).call(handler)
        val presenter = TgUser(message.from)
        val luckyOne = TgUser(message.replyToMessage.from)
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.chatId)
                .setText("\uD83C\uDF82 ${luckyOne.name} пользователь ${presenter.name} подарил вам тортик " +
                        message.text.replace("/cake", ""))
                .setReplyToMessageId(message.replyToMessage.messageId)
                .setReplyMarkup(markup))
    }

    fun dice(message: Message) {
        val random: Int
        val args = message.text.split("\\s+".toRegex(), 3)
        random = when (args.size) {
            3 -> {
                try {
                    val min = args[1].toInt()
                    val max = args[2].toInt()
                    ThreadLocalRandom.current().nextInt(min, max + 1)
                } catch (nfe: NumberFormatException) {
                    ThreadLocalRandom.current().nextInt(1, 7)
                }
            }
            2 -> {
                val max = args[1].toInt()
                ThreadLocalRandom.current().nextInt(1, max + 1)
            }
            else -> ThreadLocalRandom.current().nextInt(1, 7)
        }
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.chatId)
                .setText("\uD83C\uDFB2 Кубик брошен. Результат: $random")
                .setReplyToMessageId(message.messageId))
    }

    fun marryme(message: Message) {
        val marryById = message.text.trim().matches(Regex("/marryme\\s+\\d+"))
        val chatId = message.chatId
        val userId = message.from.id
        val text: String
        val loverId: Int
        if (Services.db.getLover(userId) != 0) {
            handler.sendMessage(chatId, "Всмысле? Вы что, хотите изменить своей второй половинке?!")
            return
        }

        if (!marryById) {
            if (!message.isReply
                    || message.from.id == message.replyToMessage.from.id || message.replyToMessage.from.bot) return
            loverId = message.replyToMessage.from.id
            val user = TgUser(Methods.getChatMember(chatId, userId).call(handler).user)
            text = "Пользователь " + user.link + " предлагает вам руку, сердце и шавуху. Вы согласны?"

        } else {
            if (message.isUserMessage) return
            loverId = try {
                message.text.split(" ")[1].toInt()
            } catch (e: NumberFormatException) {
                handler.sendMessage(chatId, "Неверный формат!")
                return
            }
            val user = TgUser(Methods.getChatMember(chatId, userId).call(handler).user)
            val lover = TgUser(Methods.getChatMember(chatId, loverId).call(handler).user)
            text = "${lover.link}, пользователь ${user.link} предлагает вам руку, сердце и шавуху. Вы согласны?"
        }
        if (Services.db.getLover(loverId) != 0) {
            handler.sendMessage(chatId, "У этого пользователя уже есть своя вторая половинка!")
            return
        }
        val markup = InlineKeyboardMarkup()
        markup.keyboard = listOf(listOf(
                InlineKeyboardButton()
                        .setText("Принять")
                        .setCallbackData(LastkatkaBot.CALLBACK_ACCEPT_MARRIAGE + "$userId $loverId"),
                InlineKeyboardButton()
                        .setText("Отказаться")
                        .setCallbackData(LastkatkaBot.CALLBACK_DENY_MARRIAGE + "$userId $loverId")
        ))
        val sm = Methods.sendMessage()
                .setChatId(chatId)
                .setText(text)
                .setReplyMarkup(markup)
        if (!marryById) {
            sm.replyToMessageId = message.replyToMessage.messageId
        }
        handler.sendMessage(sm)
    }

    fun divorce(message: Message) {
        val chatId = message.chatId
        val userId = message.from.id
        val loverId = Services.db.getLover(userId)
        if (loverId == 0) {
            handler.sendMessage(chatId, "У вас и так никого нет!")
            return
        }
        Services.db.divorce(userId)
        handler.sendMessage(chatId, "Вы расстались со своей половинкой! А ведь так все хорошо начиналось...")
        val user = TgUser(Methods.getChatMember(userId.toLong(), userId).call(handler).user)
        handler.sendMessage(loverId, "Ваша половинка (${user.link}) покинула вас... Теперь вы одни...")
    }

    fun stats(message: Message) {
        val player = if (!message.isReply) message.from else message.replyToMessage.from
        if (player.bot) {
            handler.sendMessage(message.chatId, "Но это же просто бот, имитация человека! " +
                    "Разве может бот написать симфонию, иметь статистику, играть в BnC, любить?")
            return
        }
        val user = TgUser(player)
        val stats = Services.db.getStats(player.id)
        val (_, duelWins, totalDuels, bnc, loverId) = stats
        val winRate = if (totalDuels == 0) 0 else 100 * duelWins / totalDuels
        var text = """
            📊 Статистика ${user.name}:

            Дуэлей выиграно: $duelWins
            Всего дуэлей: $totalDuels
            Винрейт: $winRate%
            
            🐮 Баллов за быки и коровы: $bnc
        """.trimIndent()
        if (loverId != 0) {
            text += "\n❤️ Вторая половинка: " +
                    TgUser(Methods.getChatMember(loverId.toLong(), loverId).call(handler).user).link
        }
        handler.sendMessage(message.chatId, text)
    }

    fun pinList(message: Message) {
        if (!isFromWwBot(message)) return
        Methods.Administration.pinChatMessage(message.chatId, message.replyToMessage.messageId)
                .setNotificationEnabled(false).call(handler)
        Methods.deleteMessage(message.chatId, message.messageId).call(handler)
    }

    fun getInfo(message: Message) {
        if (!message.isReply) return

        val replacements = mapOf(
                "[ ,]*\\w+='?null'?" to "",
                "(\\w*[iI]d=)(-?\\d+)" to "$1<code>$2</code>",
                "([{,])" to "$1\n",
                "(})" to "\n$1",
                "(=)" to " $1 "
        )
        val text = StringBuilder(message.replyToMessage.toString())
        for ((old, new) in replacements) text.replace(old.toRegex(), new)
        handler.sendMessage(message.chatId, text.toString())
    }

    fun weather(message: Message) {
        val chatId = message.chatId
        var city: String? = message.text.trim().replace("/weather[_\\d\\w@]*\\s*".toRegex(), "")
        if (city!!.isBlank()) { // city is not specified
            city = Services.db.getUserCity(message.from.id)
            if (city == null) {
                handler.sendMessage(chatId, "Вы не указали город!")
                return
            }
        } else { // find a city
            try {
                val searchPage = Jsoup.parse(URL("https://yandex.ru/pogoda/search?request=" + URLEncoder.encode(city, StandardCharsets.UTF_8)), 10000)
                val table = searchPage.selectFirst("div.grid")
                val searchResult = table.selectFirst("li.place-list__item")
                city = searchResult.selectFirst("a").attr("href")
            } catch (e: NullPointerException) {
                handler.sendMessage(chatId, "Город не найден")
                return
            } catch (e: IOException) {
                handler.sendMessage(chatId, "Ошибка запроса")
            }
        }
        Services.db.setUserCity(message.from.id, city!!)
        val weatherPage: Document
        weatherPage = try {
            Jsoup.parse(URL("https://yandex.ru$city"), 10000)
        } catch (e: IOException) {
            handler.sendMessage(chatId, "Ошибка запроса")
            return
        }
        // parse weather
        val table = weatherPage.selectFirst("div.card_size_big")
        val title = weatherPage.selectFirst("h1.header-title__title").text()
        val temperature = table.selectFirst("div.fact__temp").selectFirst("span.temp__value").text()
        val feelsLike = table.selectFirst("div.fact__feels-like").selectFirst("div.term__value").text()
        val feelings = table.selectFirst("div.fact__feelings").selectFirst("div.link__condition").text()
        val wind = table.selectFirst("div.fact__wind-speed").selectFirst("div.term__value").text()
        val humidity = table.selectFirst("div.fact__humidity").selectFirst("div.term__value").text()
        val pressure = table.selectFirst("div.fact__pressure").selectFirst("div.term__value").text()
        val forecast = """
            <b>$title</b>
            
            $feelings
            🌡: $temperature °C
            🤔 Ощущается как $feelsLike
            💨: $wind
            💧: $humidity
            🧭: $pressure
            """.trimIndent()

        handler.sendMessage(chatId, forecast)
    }

    fun feedback(message: Message) {
        val user = TgUser(message.from)
        val bugreport = ("⚠️ <b>Фидбек</b>\n\n" +
                "От: ${user.link}\n\n" +
                message.text.replace("/feedback ", ""))
        handler.sendMessage(Services.botConfig.mainAdmin, bugreport)
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.chatId)
                .setText("✅ Отправлено разрабу бота!")
                .setReplyToMessageId(message.messageId))
    }

    fun bncTop(message: Message) {
        val chatId = message.chatId
        val top = Services.db.getTop()
        val text = StringBuilder("<b>Топ-10 задротов в bnc:</b>\n\n")
        var counter = 1
        for ((playerId, score) in top) {
            val member = Methods.getChatMember(playerId.toLong(), playerId).call(handler)
            val player = BnCPlayer(playerId, member.user.firstName, score)
            text.append(counter).append(": ")
            if (message.isUserMessage) text.append(player.link) else text.append(player.name)
            text.append(" (${player.score})\n")
            counter++
        }
        handler.sendMessage(chatId, text.toString())
    }

    fun bncHelp(message: Message) {
        val sendPhoto = Methods.sendPhoto()
                .setChatId(message.chatId)
                .setFile(Services.botConfig.bncphoto)
        if (message.isReply) sendPhoto.replyToMessageId = message.replyToMessage.messageId else sendPhoto.replyToMessageId = message.messageId
        sendPhoto.call(handler)
    }

    fun help(message: Message) {
        val help = StringBuilder("Привет! Это очень полезный бот для проекта @lastkatka, который многое что умеет! Основные команды:\n\n")
        val adminHelp = StringBuilder("<b>Информация для админов бота</b>\n\n")
        val mainAdminHelp = StringBuilder("<b>Информация для главного админа бота</b>\n\n")
        val noobId = message.from.id
        for (method in handler.commands.values) {
            val annotation = method.getAnnotation(Command::class.java)
            if (!annotation.showInHelp) continue

            val helpLine = "${annotation.name} - ${annotation.desc}\n"
            if (noobId == Services.botConfig.mainAdmin && annotation.forMainAdmin)
                mainAdminHelp.append(helpLine)
            else if (handler.isFromAdmin(message) && annotation.forAllAdmins)
                adminHelp.append(helpLine)
            else
                help.append(helpLine)
            // TODO add help for premium users when needed
        }
        if (handler.isFromAdmin(message)) help.append("\n").append(adminHelp)
        if (noobId == Services.botConfig.mainAdmin) help.append("\n").append(mainAdminHelp)
        // attempt to send help to PM
        try {
            handler.execute(SendMessage(message.from.id.toLong(), help.toString())
                    .setParseMode(ParseMode.HTML))
        } catch (e: TelegramApiException) {
            handler.sendMessage(Methods.sendMessage(message.chatId, "Пожалуйста, начните диалог со мной в лс")
                    .setReplyToMessageId(message.messageId))
            return
        }
        if (!message.isUserMessage) handler.sendMessage(Methods.sendMessage(message.chatId, "✅ Помощь была отправлена вам в лс")
                .setReplyToMessageId(message.messageId))
    }

    fun pair(message: Message) {
        if (message.isUserMessage) return

        val chatId = message.chatId
        // check for existing pair
        if (Services.db.pairExistsToday(chatId)) {
            var pair = Services.db.getPairOfTheDay(chatId)
            pair = "Пара дня: $pair"
            handler.sendMessage(chatId, pair)
            return
        }
        // remove users without activity for 2 weeks and get list of actual users
        Services.db.removeOldUsers(chatId, message.date - 1209600)
        val userIds = Services.db.getChatMemebersIds(chatId)
        // generate 2 different random users
        val user1: TgUser
        val user2: TgUser
        try {
            user1 = getUserForPair(chatId, userIds)
            userIds.remove(user1.id)
            user2 = getUserForPair(chatId, userIds, user1)
        } catch (e: Exception) {
            handler.sendMessage(chatId, "Недостаточно пользователей для создания пары! Подождите, пока кто-то еще напишет в чат!")
            return
        }
        // get a random text and set up a pair
        val loveArray = Services.botConfig.loveStrings
        val loveStrings = loveArray[ThreadLocalRandom.current().nextInt(loveArray.size)].split("\n")
        try {
            for (i in 0 until loveStrings.size - 1) {
                handler.sendMessage(chatId, loveStrings[i])
                Thread.sleep(1500)
            }
        } catch (e: InterruptedException) {
            BotLogger.error("PAIR", "Ошибка таймера")
        }
        val pair = "${user1.name} ❤ ${user2.name}"
        Services.db.setPair(chatId, pair)
        handler.sendMessage(chatId, String.format(loveStrings[loveStrings.size - 1], user1.link, user2.link))
    }

    @Throws(Exception::class)
    private fun getUserForPair(chatId: Long, userIds: MutableList<Int>, first: TgUser): TgUser {
        val loverId = Services.db.getLover(first.id)
        return if (loverId in userIds) {
            TgUser(Methods.getChatMember(chatId, loverId).call(handler).user)
        } else getUserForPair(chatId, userIds)
    }

    @Throws(Exception::class)
    private fun getUserForPair(chatId: Long, userIds: MutableList<Int>): TgUser {
        if (userIds.size < 3) throw Exception("Not enough users")
        var member: ChatMember?
        do {
            val random = ThreadLocalRandom.current().nextInt(userIds.size)
            val userId = userIds[random]
            member = Methods.getChatMember(chatId, userId).call(handler)
            // delete not-found-user
            if (member == null) {
                Services.db.removeUserFromChatDB(userId, chatId)
                userIds.remove(userId)
                if (userIds.size < 3) {
                    throw Exception("Not enough users")
                }
            }
        } while (member == null || member.user.firstName.isBlank())
        return TgUser(member.user)
    }

    fun lastpairs(message: Message) {
        if (message.isUserMessage) return
        val chatId = message.chatId
        val history = Services.db.getPairsHistory(chatId)
        if (history == null)
            handler.sendMessage(chatId, "В этом чате еще никогда не запускали команду /pair!")
        else
            handler.sendMessage(chatId, "<b>Последние 10 пар:</b>\n\n$history")
    }

    private fun isFromWwBot(message: Message): Boolean {
        return message.replyToMessage.from.userName in Services.botConfig.wwBots &&
                message.replyToMessage.text.startsWith("#players")
    }

    companion object {
        val markupForPayingRespects: InlineKeyboardMarkup
            get() {
                val markup = InlineKeyboardMarkup()
                markup.keyboard = listOf(listOf(
                        InlineKeyboardButton()
                                .setText("F")
                                .setCallbackData(LastkatkaBot.CALLBACK_PAY_RESPECTS)
                ))
                return markup
            }
    }

}