package com.flipit.dao;

import com.flipit.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CardProgressDAO {
    public void setAnswered(int userId, int cardId, boolean answered, String selectedAnswer) {
        String sql = "INSERT INTO card_progress (user_id, card_id, answered, selected_answer) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE answered = ?, selected_answer = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, cardId);
            ps.setBoolean(3, answered);
            ps.setString(4, selectedAnswer);
            ps.setBoolean(5, answered);
            ps.setString(6, selectedAnswer);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error setting card answer", e);
        }
    }

    public int getAnsweredCount(int userId, int deckId) {
        String sql = "SELECT COUNT(*) FROM card_progress cp " +
                "JOIN cards ca ON ca.id = cp.card_id " +
                "WHERE cp.user_id = ? AND ca.deck_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error retrieving answered count", e);
        }
        return 0;
    }

    public Map<Integer, Integer> getAnsweredCountBatch(int userId, List<Integer> deckIds) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (deckIds == null || deckIds.isEmpty()) return counts;

        String placeholders = deckIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT ca.deck_id, COUNT(*) FROM card_progress cp " +
                "JOIN cards ca ON ca.id = cp.card_id " +
                "WHERE cp.user_id = ? AND ca.deck_id IN (" + placeholders + ") " +
                "GROUP BY ca.deck_id";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int index = 2;
            for (Integer id : deckIds) {
                ps.setInt(index++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getInt(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching bulk answered counts", e);
        }
        return counts;
    }

    public int getCorrectCount(int userId, int deckId) {
        String sql = "SELECT COUNT(*) FROM card_progress cp " +
                "JOIN cards ca ON ca.id = cp.card_id " +
                "WHERE cp.user_id = ? AND ca.deck_id = ? AND cp.answered = TRUE " +
                "AND cp.selected_answer = ca.correct_answer";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error retrieving correct answers count", e);
        }
        return 0;
    }

    public void resetProgress(int userId, int deckId) {
        String sql = "DELETE cp FROM card_progress cp " +
                "JOIN cards ca ON cp.card_id = ca.id " +
                "WHERE cp.user_id = ? AND ca.deck_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Database error resetting user progress", e);
        }
    }

    public Map<Integer, Integer> getDeckViewsBatch(List<Integer> deckIds) {
        Map<Integer, Integer> viewsMap = new HashMap<>();
        if (deckIds == null || deckIds.isEmpty()) return viewsMap;

        String placeholders = deckIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT ca.deck_id, COUNT(DISTINCT cp.user_id) FROM card_progress cp " +
                "JOIN cards ca ON ca.id = cp.card_id " +
                "WHERE ca.deck_id IN (" + placeholders + ") " +
                "GROUP BY ca.deck_id";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int index = 1;
            for (Integer id : deckIds) {
                ps.setInt(index++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    viewsMap.put(rs.getInt(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching bulk deck views", e);
        }
        return viewsMap;
    }

    public Map<Integer, String> getSelectedAnswersBatch(int userId, List<Integer> cardIds) {
        Map<Integer, String> answersMap = new HashMap<>();
        if (cardIds == null || cardIds.isEmpty()) return answersMap;

        String placeholders = cardIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT card_id, selected_answer FROM card_progress " +
                "WHERE user_id = ? AND card_id IN (" + placeholders + ")";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            int index = 2;
            for (Integer id : cardIds) {
                ps.setInt(index++, id);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answersMap.put(rs.getInt("card_id"), rs.getString("selected_answer"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error bulk fetching answers", e);
        }
        return answersMap;
    }

    public void updateBestScore(int userId, int deckId, int currentScore, int totalCards) {
        String sql = "INSERT INTO highscores (user_id, deck_id, best_score, total_cards) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "best_score = IF((? / NULLIF(?, 0)) > (best_score / NULLIF(total_cards, 0)), ?, best_score), " +
                "total_cards = IF((? / NULLIF(?, 0)) > (best_score / NULLIF(total_cards, 0)), ?, total_cards)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            ps.setInt(3, currentScore);
            ps.setInt(4, totalCards);

            ps.setDouble(5, currentScore);
            ps.setDouble(6, totalCards);
            ps.setInt(7, currentScore);

            ps.setDouble(8, currentScore);
            ps.setDouble(9, totalCards);
            ps.setInt(10, totalCards);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating best score", e);
        }
    }

    public int getBestScorePct(int userId, int deckId) {
        String sql = "SELECT best_score, total_cards FROM highscores WHERE user_id = ? AND deck_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int best = rs.getInt("best_score");
                    int total = rs.getInt("total_cards");
                    return total == 0 ? 100 : (best * 100 / total);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error retrieving best score percentage", e);
        }
        return 0;
    }

    public List<Object[]> getLeaderboard(int deckId) {
        List<Object[]> results = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.profile_picture, h.best_score, h.total_cards, h.saved_at " +
                "FROM highscores h " +
                "JOIN users u ON h.user_id = u.id " +
                "WHERE h.deck_id = ? " +
                "ORDER BY (h.best_score / NULLIF(h.total_cards, 0)) DESC, h.best_score DESC, h.saved_at ASC " +
                "LIMIT 50";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    int userId = rs.getInt("id");
                    String username = rs.getString("username");
                    byte[] pic = rs.getBytes("profile_picture");
                    int best = rs.getInt("best_score");
                    int total = rs.getInt("total_cards");
                    Timestamp date = rs.getTimestamp("saved_at");
                    results.add(new Object[]{rank++, userId, username, pic, best, total, date});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public void syncDeckHighscores(int deckId) {
        String sql = "UPDATE highscores h " +
                "JOIN (SELECT COUNT(*) as current_total FROM cards WHERE deck_id = ?) c " +
                "SET h.total_cards = c.current_total, " +
                "    h.best_score = LEAST(h.best_score, c.current_total) " +
                "WHERE h.deck_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            ps.setInt(2, deckId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error syncing highscores", e);
        }
    }

    public void resetSingleCardProgress(int cardId) {
        String sql = "DELETE FROM card_progress WHERE card_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error resetting single card progress", e);
        }
    }
}