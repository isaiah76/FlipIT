package com.flipit.models;

public class Card {
    private int id;
    private int deckId;
    private String question;
    private String answerA, answerB, answerC, answerD;
    private String correctAnswer;

    public Card(int id, int deckId, String question, String a, String b, String c, String d, String correct) {
        this.id = id;
        this.deckId = deckId;
        this.question = question;
        this.answerA = a;
        this.answerB = b;
        this.answerC = c;
        this.answerD = d;
        this.correctAnswer = correct;
    }

    public Card(int deckId, String question, String a, String b, String c, String d, String correct) {
        this(0, deckId, question, a, b, c, d, correct);
    }

    public int getId() {
        return id;
    }

    public int getDeckId() {
        return deckId;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswerA() {
        return answerA;
    }

    public String getAnswerB() {
        return answerB;
    }

    public String getAnswerC() {
        return answerC;
    }

    public String getAnswerD() {
        return answerD;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getAnswer(String letter) {
        switch (letter) {
            case "A":
                return answerA;
            case "B":
                return answerB;
            case "C":
                return answerC;
            default:
                return answerD;
        }
    }

    public void setQuestion(String q) {
        question = q;
    }

    public void setAnswerA(String a) {
        answerA = a;
    }

    public void setAnswerB(String b) {
        answerB = b;
    }

    public void setAnswerC(String c) {
        answerC = c;
    }

    public void setAnswerD(String d) {
        answerD = d;
    }

    public void setCorrectAnswer(String ca) {
        correctAnswer = ca;
    }
}