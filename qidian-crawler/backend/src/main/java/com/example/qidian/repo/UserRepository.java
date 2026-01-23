package com.example.qidian.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.example.qidian.domain.User;

@Repository
public class UserRepository {

    private final DataSource dataSource;

    @Autowired
    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password, phone, email, role FROM user WHERE username = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                User u = new User();
                u.setid(rs.getLong("id"));
                u.setusername(rs.getString("username"));
                u.setpassword(rs.getString("password"));
                u.setphone(rs.getString("phone"));
                u.setemail(rs.getString("email"));
                u.setrole(rs.getString("role"));
                return Optional.of(u);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int insertUser(User user) {
        String sql = "INSERT INTO user (username, password, phone, email, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getusername());
            ps.setString(2, user.getpassword());
            ps.setString(3, user.getphone());
            ps.setString(4, user.getemail());
            ps.setString(5, user.getrole());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                return rows;
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setid(rs.getLong(1));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int deleteUser(User user) {
        String sql = "DELETE FROM user WHERE username=?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getusername());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> updateUser(User user) {
        String sql = "UPDATE user SET username=?, password=?, phone=?, email=?, role=? WHERE username=?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getusername());
            ps.setString(2, user.getpassword());
            ps.setString(3, user.getphone());
            ps.setString(4, user.getemail());
            ps.setString(5, user.getrole());
            ps.setString(6, user.getusername());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                return Optional.empty();
            }
            return Optional.of(user);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> findByphone(String phone) {
        String sql = "SELECT id, username, password, phone, email, role FROM user where phone= ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                } else {
                    User u = new User();
                    u.setid(rs.getLong("id"));
                    u.setusername(rs.getString("username"));
                    u.setpassword(rs.getString("password"));
                    u.setphone(rs.getString("phone"));
                    u.setemail(rs.getString("email"));
                    u.setrole(rs.getString("role"));
                    return Optional.of(u);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
