package com.flipit.dao;

import com.flipit.db.DBConnection;
import com.flipit.models.Card;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CardDAO {
    public int addCard(Card card) {
        String sql = "INSERT INTO cards (deck_id, question, answer_a, answer_b, answer_c, answer_d, correct_answer) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, card.getDeckId());
            ps.setString(2, card.getQuestion());
            ps.setString(3, card.getAnswerA());
            ps.setString(4, card.getAnswerB());
            ps.setString(5, card.getAnswerC());
            ps.setString(6, card.getAnswerD());
            ps.setString(7, card.getCorrectAnswer());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error adding card", e);
        }
        return -1;
    }

    public void addCardsBatch(int deckId, List<Card> cards) {
        if (cards == null || cards.isEmpty()) return;

        String sql = "INSERT INTO cards (deck_id, question, answer_a, answer_b, answer_c, answer_d, correct_answer) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Card card : cards) {
                ps.setInt(1, deckId);
                ps.setString(2, card.getQuestion());
                ps.setString(3, card.getAnswerA());
                ps.setString(4, card.getAnswerB());
                ps.setString(5, card.getAnswerC());
                ps.setString(6, card.getAnswerD());
                ps.setString(7, card.getCorrectAnswer());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Database error batch inserting cards", e);
        }
    }

    public boolean updateCard(Card card) {
        String sql = "UPDATE cards SET question=?, answer_a=?, answer_b=?, " +
                "answer_c=?, answer_d=?, correct_answer=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, card.getQuestion());
            ps.setString(2, card.getAnswerA());
            ps.setString(3, card.getAnswerB());
            ps.setString(4, card.getAnswerC());
            ps.setString(5, card.getAnswerD());
            ps.setString(6, card.getCorrectAnswer());
            ps.setInt(7, card.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating card", e);
        }
    }

    public boolean deleteCard(int cardId) {
        String sql = "DELETE FROM cards WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error deleting card", e);
        }
    }

    public List<Card> getAllCardsByDeck(int deckId) {
        List<Card> list = new ArrayList<>();
        String sql = "SELECT * FROM cards WHERE deck_id = ? ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = new Card(
                            rs.getInt("id"),
                            rs.getInt("deck_id"),
                            rs.getString("question"),
                            rs.getString("answer_a"),
                            rs.getString("answer_b"),
                            rs.getString("answer_c"),
                            rs.getString("answer_d"),
                            rs.getString("correct_answer")
                    );
                    list.add(card);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching deck cards", e);
        }
        return list;
    }
}