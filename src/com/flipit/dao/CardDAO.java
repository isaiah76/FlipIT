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
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
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
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteCard(int cardId) {
        String sql = "DELETE FROM cards WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Card> getAllCardsByDeck(int deckId) {
        List<Card> list = new ArrayList<>();
        String sql = "SELECT * FROM cards WHERE deck_id = ? ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            ResultSet rs = ps.executeQuery();
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

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}