package com.flipit.dao;

import com.flipit.db.DBConnection;
import com.flipit.models.Deck;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeckDAO {
    public int createDeck(int userId, String title, String description) {
        String sql = "INSERT INTO decks (user_id, title, description) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateDeck(int deckId, String title, String description) {
        String sql = "UPDATE decks SET title=?, description=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setInt(3, deckId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteDeck(int deckId) {
        String sql = "DELETE FROM decks WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Deck> getAllDecksByUser(int userId) {
        List<Deck> list = new ArrayList<>();
        String sql = "SELECT d.*, (SELECT COUNT(*) FROM cards c WHERE c.deck_id = d.id) AS card_count " +
                "FROM decks d WHERE d.user_id = ? ORDER BY d.created_at DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Deck deck = new Deck(rs.getInt("id"), rs.getInt("user_id"),
                        rs.getString("title"), rs.getString("description"));
                deck.setCardCount(rs.getInt("card_count"));
                list.add(deck);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Deck getAllDecksById(int deckId) {
        String sql = "SELECT * FROM decks WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Deck(rs.getInt("id"), rs.getInt("user_id"),
                        rs.getString("title"), rs.getString("description"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}