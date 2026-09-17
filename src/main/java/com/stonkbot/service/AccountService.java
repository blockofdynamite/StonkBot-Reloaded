package com.stonkbot.service;

import com.stonkbot.db.Database;
import com.stonkbot.db.Holding;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AccountService {
    public static final BigDecimal STARTING_BALANCE = new BigDecimal("1000.00");

    private final Database db;

    public AccountService(Database db) {
        this.db = db;
    }

    public synchronized void ensureAccount(String guildId, String userId) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "INSERT OR IGNORE INTO accounts (guild_id, user_id, cash) VALUES (?, ?, ?)")) {
            ps.setString(1, guildId);
            ps.setString(2, userId);
            ps.setBigDecimal(3, STARTING_BALANCE);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        }
    }

    public synchronized BigDecimal cash(String guildId, String userId) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT cash FROM accounts WHERE guild_id = ? AND user_id = ?")) {
            ps.setString(1, guildId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        }
    }

    public synchronized List<Holding> holdings(String guildId, String userId) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT symbol, shares, avg_cost FROM holdings WHERE guild_id = ? AND user_id = ? ORDER BY symbol")) {
            ps.setString(1, guildId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Holding> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new Holding(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3)));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        }
    }

    public synchronized Optional<Holding> findHolding(String guildId, String userId, String symbol) {
        return holdings(guildId, userId).stream()
                .filter(h -> h.symbol().equals(symbol))
                .findFirst();
    }

    public synchronized void applyBuy(String guildId, String userId, String symbol, BigDecimal shares, BigDecimal price) {
        Connection c = db.connection();
        try {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE accounts SET cash = cash - ? WHERE guild_id = ? AND user_id = ?")) {
                ps.setBigDecimal(1, price.multiply(shares));
                ps.setString(2, guildId);
                ps.setString(3, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO holdings (guild_id, user_id, symbol, shares, avg_cost)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (guild_id, user_id, symbol) DO UPDATE SET
                        shares   = holdings.shares + excluded.shares,
                        avg_cost = (holdings.shares * holdings.avg_cost + excluded.shares * excluded.avg_cost)
                                   / (holdings.shares + excluded.shares)
                    """)) {
                ps.setString(1, guildId);
                ps.setString(2, userId);
                ps.setString(3, symbol);
                ps.setBigDecimal(4, shares);
                ps.setBigDecimal(5, price);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public synchronized void applySell(String guildId, String userId, String symbol, BigDecimal shares, BigDecimal price) {
        Connection c = db.connection();
        try {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE accounts SET cash = cash + ? WHERE guild_id = ? AND user_id = ?")) {
                ps.setBigDecimal(1, price.multiply(shares));
                ps.setString(2, guildId);
                ps.setString(3, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE holdings SET shares = shares - ? WHERE guild_id = ? AND user_id = ? AND symbol = ?")) {
                ps.setBigDecimal(1, shares);
                ps.setString(2, guildId);
                ps.setString(3, userId);
                ps.setString(4, symbol);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM holdings WHERE guild_id = ? AND user_id = ? AND symbol = ? AND shares <= 0")) {
                ps.setString(1, guildId);
                ps.setString(2, userId);
                ps.setString(3, symbol);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public synchronized void reset(String guildId, String userId) {
        Connection c = db.connection();
        try {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE accounts SET cash = ? WHERE guild_id = ? AND user_id = ?")) {
                ps.setBigDecimal(1, STARTING_BALANCE);
                ps.setString(2, guildId);
                ps.setString(3, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM holdings WHERE guild_id = ? AND user_id = ?")) {
                ps.setString(1, guildId);
                ps.setString(2, userId);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }
}
