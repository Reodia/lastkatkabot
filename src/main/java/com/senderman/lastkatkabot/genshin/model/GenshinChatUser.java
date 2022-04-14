package com.senderman.lastkatkabot.genshin.model;

import org.springframework.data.annotation.TypeAlias;

@TypeAlias("genshinChatUser")
public class GenshinChatUser {

    private long chatId;
    private long userId;
    private int lastRollDate;
    private int fourPity;
    private int fivePity;

    public GenshinChatUser() {
    }

    public GenshinChatUser(long chatId, long userId) {
        this.chatId = chatId;
        this.userId = userId;
        this.lastRollDate = 0;
        this.fourPity = 0;
        this.fivePity = 0;

    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getLastRollDate() {
        return lastRollDate;
    }

    public void setLastRollDate(int lastRollDate) {
        this.lastRollDate = lastRollDate;
    }

    public int getFourPity() {
        return fourPity;
    }

    public void setFourPity(int fourPity) {
        this.fourPity = fourPity;
    }

    public int getFivePity() {
        return fivePity;
    }

    public void setFivePity(int fivePity) {
        this.fivePity = fivePity;
    }

    public void incFourPity() {
        fourPity++;
    }

    public void incFivePity() {
        fivePity++;
    }
}
