package ru.job4j.store;

import org.apache.commons.dbcp2.BasicDataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import ru.job4j.model.Account;
import ru.job4j.model.Ticket;

public class DbStore {
    private final BasicDataSource pool = new BasicDataSource();

    private DbStore() {
        Properties cfg = new Properties();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                                DbStore.class.getClassLoader()
                                .getResourceAsStream("db.properties"))))) {
            cfg.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        pool.setDriverClassName(cfg.getProperty("jdbc.driver"));
        pool.setUrl(cfg.getProperty("jdbc.url"));
        pool.setUsername(cfg.getProperty("jdbc.username"));
        pool.setPassword(cfg.getProperty("jdbc.password"));
        pool.setMinIdle(5);
        pool.setMaxIdle(10);
        pool.setMaxOpenPreparedStatements(100);
    }

    private static class Holder {
        private static final DbStore INSTANCE = new DbStore();
    }

    public static DbStore getInstance() {
        return Holder.INSTANCE;
    }

    public List<Ticket> findAllTickets() {
        List<Ticket> result = new ArrayList<>();
        try (Connection con = pool.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM ticket")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ticket ticket = new Ticket(
                            rs.getInt("session_id"),
                            rs.getInt("row"),
                            rs.getInt("col"),
                            rs.getInt("account_id"));
                    result.add(ticket);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void createTicket(Ticket ticket) throws SQLException {
        try (Connection cn = pool.getConnection();
            PreparedStatement ps = cn.prepareStatement(
        "INSERT INTO ticket (session_id, row, col, account_id) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, 1);
            ps.setInt(2, ticket.getRow());
            ps.setInt(3, ticket.getCol());
            ps.setInt(4, ticket.getAccountId());
            ps.executeUpdate();
        }
    }

    public int findAccount(Account account) {
        int result = -1;
        try (Connection cn = pool.getConnection();
            PreparedStatement ps = cn.prepareStatement(
        "SELECT id FROM account WHERE email = ? AND phone = ?")) {
            ps.setString(1, account.getEmail());
            ps.setString(2, account.getPhone());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public int createAccount(Account account) throws SQLException {
        int result = -1;
        try (Connection cn = pool.getConnection();
            PreparedStatement ps = cn.prepareStatement(
        "INSERT INTO account (username, email, phone) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, account.getName());
            ps.setString(2, account.getEmail());
            ps.setString(3, account.getPhone());
            ps.execute();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    result = rs.getInt(1);
                }
            }
        }
        return result;
    }
}