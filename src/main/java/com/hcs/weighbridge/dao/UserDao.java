package com.hcs.weighbridge.dao;

import com.hcs.weighbridge.model.User;
import com.hcs.weighbridge.util.LogUtil;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    private static final Logger logger = LogUtil.getLogger(UserDao.class);
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
                            rs.getString("role"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
        }
        return null;
    }

    public boolean validateUser(String username, String password) {
        User user = findByUsername(username);
        if (user != null) {
            // In a real application, you should hash passwords.
            // For now, we are comparing plain text as per the request context implies
            // simple auth.
            return user.getPassword().equals(password);
        }
        return false;
    }
}
