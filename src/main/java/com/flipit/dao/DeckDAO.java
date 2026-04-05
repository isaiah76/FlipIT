package com.flipit.dao;

import com.flipit.db.DBConnection;
import com.flipit.models.Deck;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DeckDAO {
    public int createDeck(int userId, Integer fileId, String title, String description, boolean isPublic) {
        String sql = "INSERT INTO decks (user_id, file_id, title, description, is_public) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            if (fileId != null) ps.setInt(2, fileId);
            else ps.setNull(2, Types.INTEGER);
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setBoolean(5, isPublic);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error creating deck", e);
        }
        return -1;
    }

    public boolean updateDeck(int deckId, String title, String description, boolean isPublic) {
        String sql = "UPDATE decks SET title=?, description=?, is_public=?, " +
                "published_at = CASE " +
                "WHEN ? = TRUE AND published_at IS NULL THEN CURRENT_TIMESTAMP " +
                "ELSE published_at END " +
                "WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setBoolean(3, isPublic);
            ps.setBoolean(4, isPublic);
            ps.setInt(5, deckId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating deck", e);
        }
    }

    public void setDeckDisabled(int deckId, boolean isDisabled) {
        String sql = "UPDATE decks SET is_disabled = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, isDisabled);
            ps.setInt(2, deckId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error disabling deck", e);
        }
    }

    public boolean deleteDeck(int deckId) {
        String sql = "DELETE FROM decks WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error deleting deck", e);
        }
    }

    public List<Deck> getPublicDecks() {
        List<Deck> list = new ArrayList<>();
        String sql = "SELECT * FROM view_deck_count WHERE is_public = TRUE AND is_disabled = FALSE ORDER BY published_at DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToDeck(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching public decks", e);
        }
        return list;
    }

    public List<Deck> getAllSystemDecks() {
        List<Deck> list = new ArrayList<>();
        String sql = "SELECT * FROM view_deck_count ORDER BY created_at DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToDeck(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching system decks", e);
        }
        return list;
    }

    public List<Deck> getAllDecksByUser(int userId) {
        List<Deck> list = new ArrayList<>();
        String sql = "SELECT * FROM view_deck_count WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDeck(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching user decks", e);
        }
        return list;
    }

    public List<Deck> getSavedDecksByUser(int userId) {
        List<Deck> list = new ArrayList<>();
        String sql = "SELECT v.* FROM view_deck_count v " +
                "JOIN saved_decks s ON v.id = s.deck_id " +
                "WHERE s.user_id = ? ORDER BY s.saved_at DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDeck(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching saved decks", e);
        }
        return list;
    }

    public boolean saveDeck(int userId, int deckId) {
        String sql = "INSERT IGNORE INTO saved_decks (user_id, deck_id) " +
                "SELECT ?, ? FROM decks WHERE id = ? AND user_id != ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            ps.setInt(3, deckId);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving deck", e);
        }
    }

    public boolean unsaveDeck(int userId, int deckId) {
        String sql = "DELETE FROM saved_decks WHERE user_id = ? AND deck_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error unsaving deck", e);
        }
    }

    public Map<Integer, Integer> getDeckSavesBatch(List<Integer> deckIds) {
        Map<Integer, Integer> savesMap = new HashMap<>();
        if (deckIds == null || deckIds.isEmpty()) return savesMap;

        String placeholders = deckIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT deck_id, COUNT(*) FROM saved_decks WHERE deck_id IN (" + placeholders + ") GROUP BY deck_id";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int index = 1;
            for (Integer id : deckIds) {
                ps.setInt(index++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    savesMap.put(rs.getInt(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching bulk saves", e);
        }
        return savesMap;
    }

    public Set<Integer> getSavedDeckIdsBatch(int userId, List<Integer> deckIds) {
        Set<Integer> savedSet = new HashSet<>();
        if (deckIds == null || deckIds.isEmpty()) return savedSet;

        String placeholders = deckIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT deck_id FROM saved_decks WHERE user_id = ? AND deck_id IN (" + placeholders + ")";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int index = 2;
            for (Integer id : deckIds) {
                ps.setInt(index++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    savedSet.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching bulk saved deck IDs", e);
        }
        return savedSet;
    }

    public boolean isUserTitleTaken(int userId, String title, int excludeDeckId) {
        String sql = "SELECT 1 FROM decks WHERE user_id = ? AND LOWER(title) = LOWER(?) AND id != ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setInt(3, excludeDeckId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error checking user title", e);
        }
    }

    public boolean isPublicTitleTaken(String title, int excludeDeckId) {
        String sql = "SELECT 1 FROM decks WHERE is_public = TRUE AND LOWER(title) = LOWER(?) AND id != ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, excludeDeckId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error checking public title", e);
        }
    }

    public void updateDeckTags(int deckId, List<String> tags) {
        String deleteSql = "DELETE FROM deck_tags WHERE deck_id = ?";
        String insertSql = "INSERT IGNORE INTO deck_tags (deck_id, tag_name) VALUES (?, ?)";

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement psDelete = c.prepareStatement(deleteSql)) {
                psDelete.setInt(1, deckId);
                psDelete.executeUpdate();
            }

            if (tags != null && !tags.isEmpty()) {
                try (PreparedStatement psInsert = c.prepareStatement(insertSql)) {
                    for (String tag : tags) {
                        if (tag != null && !tag.trim().isEmpty()) {
                            psInsert.setInt(1, deckId);
                            psInsert.setString(2, tag.trim().toLowerCase());
                            psInsert.addBatch();
                        }
                    }
                    psInsert.executeBatch();
                }
            }
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating deck tags", e);
        }
    }

    private Deck mapRowToDeck(ResultSet rs) throws SQLException {
        Deck deck = new Deck(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBoolean("is_public")
        );
        deck.setCardCount(rs.getInt("card_count"));
        deck.setCreatedAt(rs.getTimestamp("created_at"));
        deck.setPublishedAt(rs.getTimestamp("published_at"));
        deck.setCreatorName(rs.getString("creator_name"));

        try {
            deck.setDisabled(rs.getBoolean("is_disabled"));
        } catch (SQLException ignored) {
        }

        try {
            deck.setFileId(rs.getInt("file_id"));
            deck.setSourceFileName(rs.getString("source_file_name"));
        } catch (SQLException ignored) {
        }

        try {
            String tagsStr = rs.getString("tags");
            if (tagsStr != null && !tagsStr.isEmpty()) {
                deck.setTags(java.util.Arrays.asList(tagsStr.split(",")));
            }
        } catch (SQLException ignored) {
        }

        return deck;
    }
}