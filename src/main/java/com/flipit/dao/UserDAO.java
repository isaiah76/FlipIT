package com.flipit.dao;

import com.flipit.db.DBConnection;
import com.flipit.models.User;
import com.flipit.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class UserDAO {
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    if (PasswordUtil.verify(password, stored)) {
                        boolean active = rs.getBoolean("is_active");
                        if (!active) return null;
                        User u = new User(rs.getInt("id"), rs.getString("username"),
                                rs.getString("role"), true);
                        u.setCreatedAt(rs.getTimestamp("created_at"));
                        return u;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during login", e);
        }
        return null;
    }

    public boolean signup(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error during signup", e);
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error checking username existence", e);
        }
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getBoolean("is_active")
                    );
                    u.setCreatedAt(rs.getTimestamp("created_at"));
                    return u;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error retrieving user by username", e);
        }
        return null;
    }

    public Timestamp getDeviceLockoutTime(String deviceId, String username) {
        String sql = "SELECT locked_until FROM device_blocks WHERE device_id = ? AND username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, deviceId);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getTimestamp("locked_until");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error retrieving lockout time", e);
        }
        return null;
    }

    public int recordFailedDeviceAttempt(String deviceId, String username) {
        String upsertSql =
                "INSERT INTO device_blocks (device_id, username, failed_attempts, last_failed_attempt) " +
                        "VALUES (?, ?, 1, NOW()) " + "ON DUPLICATE KEY UPDATE " +
                        "    failed_attempts = CASE " + "        WHEN last_failed_attempt IS NULL OR TIMESTAMPDIFF(SECOND, last_failed_attempt, NOW()) > 1800 " + "        THEN 1 " + "        ELSE failed_attempts + 1 " +
                        "    END, " + "    locked_until = CASE " + "        WHEN (CASE WHEN last_failed_attempt IS NULL OR TIMESTAMPDIFF(SECOND, last_failed_attempt, NOW()) > 1800 " + "              THEN 1 ELSE failed_attempts + 1 END) >= 9 " + "        THEN DATE_ADD(NOW(), INTERVAL 5 MINUTE) " + "        ELSE NULL " + "    END, " + "    last_failed_attempt = NOW()";

        String selectSql = "SELECT failed_attempts FROM device_blocks WHERE device_id = ? AND username = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement psUpdate = c.prepareStatement(upsertSql);
             PreparedStatement psSelect = c.prepareStatement(selectSql)) {

            psUpdate.setString(1, deviceId);
            psUpdate.setString(2, username);
            psUpdate.executeUpdate();

            psSelect.setString(1, deviceId);
            psSelect.setString(2, username);
            try (ResultSet rs = psSelect.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error recording failed attempt", e);
        }
        return 0;
    }

    public void resetDeviceFailedAttempts(String deviceId, String username) {
        String sql = "DELETE FROM device_blocks WHERE device_id = ? AND username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, deviceId);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error resetting failed attempts", e);
        }
    }

    public byte[] getProfilePicture(int userId) {
        String sql = "SELECT profile_picture FROM users WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBytes("profile_picture");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error retrieving profile picture", e);
        }
        return null;
    }

    public Map<Integer, byte[]> getProfilePicturesBatch(List<Integer> userIds) {
        Map<Integer, byte[]> avatars = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return avatars;

        String placeholders = userIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, profile_picture FROM users WHERE id IN (" + placeholders + ")";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int index = 1;
            for (Integer id : userIds) {
                ps.setInt(index++, id);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byte[] pic = rs.getBytes("profile_picture");
                    if (pic != null) {
                        avatars.put(rs.getInt("id"), pic);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error getting batched profile pictures", e);
        }
        return avatars;
    }

    public boolean updateProfilePicture(int userId, byte[] imageBytes) {
        String sql = "UPDATE users SET profile_picture = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBytes(1, imageBytes);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating profile picture", e);
        }
    }

    public boolean removeProfilePicture(int userId) {
        String sql = "UPDATE users SET profile_picture = NULL WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error removing profile picture", e);
        }
    }

    public int getTotalUsers() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Database error retrieving total users", e);
        }
        return 0;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, role, is_active, created_at FROM users ORDER BY id ASC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getBoolean("is_active")
                );
                u.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(u);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error retrieving all users", e);
        }
        return list;
    }

    public boolean updateUserStatus(int userId, boolean isActive) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, isActive);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating user status", e);
        }
    }

    public boolean updateUserRole(int userId, String role) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating user role", e);
        }
    }
}