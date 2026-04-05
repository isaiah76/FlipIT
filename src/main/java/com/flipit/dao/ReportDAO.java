package com.flipit.dao;

import com.flipit.db.DBConnection;
import com.flipit.models.DeckReport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public boolean submitReport(int deckId, int userId, String reason) {
        String sql = "INSERT INTO deck_reports (deck_id, user_id, reason) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            ps.setInt(2, userId);
            ps.setString(3, reason);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error submitting report", e);
        }
    }

    public boolean hasUserReported(int userId, int deckId) {
        String sql = "SELECT 1 FROM deck_reports WHERE user_id = ? AND deck_id = ? AND status = 'pending'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error checking previous reports", e);
        }
    }

    public List<DeckReport> getPendingReports() {
        List<DeckReport> list = new ArrayList<>();
        String sql = "SELECT r.*, d.title AS deck_title, u.username AS reporter_name " +
                "FROM deck_reports r " +
                "JOIN decks d ON r.deck_id = d.id " +
                "JOIN users u ON r.user_id = u.id " +
                "WHERE r.status = 'pending' ORDER BY r.created_at ASC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DeckReport report = new DeckReport(
                        rs.getInt("id"), rs.getInt("deck_id"), rs.getInt("user_id"),
                        rs.getString("reason"), rs.getString("status"), rs.getTimestamp("created_at")
                );
                report.setDeckTitle(rs.getString("deck_title"));
                report.setReporterName(rs.getString("reporter_name"));
                list.add(report);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching pending reports", e);
        }
        return list;
    }

    public boolean updateReportStatus(int reportId, String status) {
        String sql = "UPDATE deck_reports SET status = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating report status", e);
        }
    }
}