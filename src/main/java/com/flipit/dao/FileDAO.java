package com.flipit.dao;

import com.flipit.db.DBConnection;
import com.flipit.models.UploadedFile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDAO {
    public int logFile(int userId, String fileName, String fileType, long fileSize, byte[] fileData) {
        String sql = "INSERT INTO uploaded_files (user_id, file_name, file_type, file_size, file_data) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, fileName);
            ps.setString(3, fileType);
            ps.setLong(4, fileSize);
            ps.setBytes(5, fileData);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error logging file", e);
        }
        return -1;
    }

    public boolean fileExists(int userId, String fileName) {
        return getFileIdByName(userId, fileName) > 0;
    }

    public int getFileIdByName(int userId, String fileName) {
        String sql = "SELECT id FROM uploaded_files WHERE user_id = ? AND file_name = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error getting file ID", e);
        }
        return -1;
    }

    public byte[] getFileData(int fileId) {
        String sql = "SELECT file_data FROM uploaded_files WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBytes("file_data");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error getting file data", e);
        }
        return null;
    }

    public void deleteFile(int fileId) {
        String sql = "DELETE FROM uploaded_files WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error deleting file", e);
        }
    }

    public List<UploadedFile> getAllFilesByUser(int userId) {
        List<UploadedFile> list = new ArrayList<>();
        String sql = "SELECT * FROM uploaded_files WHERE user_id = ? ORDER BY uploaded_at DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new UploadedFile(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("file_name"),
                            rs.getString("file_type"),
                            rs.getLong("file_size"),
                            rs.getTimestamp("uploaded_at")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error getting all user files", e);
        }
        return list;
    }
}