package com.hcs.weighbridge.dao;

import com.hcs.weighbridge.model.User;
import com.hcs.weighbridge.util.AppException;
import com.hcs.weighbridge.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    private final Connection connection;

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            com.hcs.weighbridge.model.Role.fromString(rs.getString("role")));
                }
            }
        } catch (SQLException e) {
            throw new AppException("Error finding user by username: " + username, e);
        }
        return null;
    }

    public boolean validateUser(String username, String password) {
        User user = findByUsername(username);
        if (user != null) {
            return SecurityUtil.checkPassword(password, user.getPassword());
        }
        return false;
    }

    public boolean createUser(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, SecurityUtil.hashPassword(user.getPassword()));
            pstmt.setString(3, user.getRole().name());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new AppException("Error creating user: " + user.getUsername(), e);
        }
    }

    public java.util.List<User> getAllUsers() {
        java.util.List<User> users = new java.util.ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        com.hcs.weighbridge.model.Role.fromString(rs.getString("role"))));
            }
        } catch (SQLException e) {
            throw new AppException("Error retrieving all users", e);
        }
        return users;
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new AppException("Error deleting user with ID: " + userId, e);
        }
    }
}
