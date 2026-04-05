package com.flipit.dao;

import com.flipit.db.DBConnection;

import java.sql.*;

public class AdminStatsDAO {
    public int[] getAllDashboardStats() {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM users WHERE role = 'user'), " +
                "(SELECT COUNT(*) FROM users WHERE role = 'user' AND is_active = TRUE), " +
                "(SELECT COUNT(*) FROM users WHERE role = 'user' AND is_active = FALSE), " +
                "(SELECT COUNT(*) FROM users WHERE role = 'user' AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)), " +
                "(SELECT COUNT(*) FROM decks), " +
                "(SELECT COUNT(*) FROM cards), " +
                "(SELECT COUNT(*) FROM decks WHERE is_public = TRUE AND is_disabled = FALSE), " +
                "(SELECT COUNT(*) FROM decks WHERE is_disabled = TRUE), " +
                "(SELECT COUNT(*) FROM deck_reports WHERE status = 'pending')";

        int[] stats = new int[9];
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                for (int i = 0; i < 9; i++) {
                    stats[i] = rs.getInt(i + 1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching bulk dashboard stats", e);
        }
        return stats;
    }
}