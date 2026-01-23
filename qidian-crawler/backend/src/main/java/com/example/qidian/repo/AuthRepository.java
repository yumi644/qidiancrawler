package com.example.qidian.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.qidian.domain.User;

public class AuthRepository {

    private final DataSource dataSource;

    @Autowired
    public AuthRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<User> register(User user) {
        String sql = "INSERT INTO user (username, password, phone, email, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getusername());
            ps.setString(2, user.getpassword());
            ps.setString(3, user.getphone());
            ps.setString(4, user.getemail());
            ps.setString(5, user.getrole());
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.of(user);
    }

    public boolean login(User user) {
        String sql = "SELECT * FROM user WHERE username=? AND password=?";
        try (Connection connec = dataSource.getConnection(); PreparedStatement ps = connec.prepareStatement(sql)) {
            ps.setString(1, user.getusername());
            ps.setString(2, user.getpassword());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
