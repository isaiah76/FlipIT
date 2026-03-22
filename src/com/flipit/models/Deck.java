package com.flipit.models;

public class Deck {
    private int id;
    private int userId;
    private String title;
    private String description;
    private int cardCount;
    private int answeredCount;

    public Deck(int id, int userId, String title, String description) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getCardCount() {
        return cardCount;
    }

    public void setCardCount(int c) {
        cardCount = c;
    }

    public int getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(int c) {
        answeredCount = c;
    }

    public void setTitle(String t) {
        title = t;
    }

    public void setDescription(String d) {
        description = d;
    }
}