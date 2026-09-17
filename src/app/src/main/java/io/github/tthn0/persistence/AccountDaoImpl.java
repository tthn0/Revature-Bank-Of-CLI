package io.github.tthn0.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.github.tthn0.domain.Account;

public class AccountDaoImpl implements AccountDao {
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM accounts WHERE account_id = ?";
    private static final String INSERT_SQL = "INSERT INTO accounts (first_name, last_name, pin) VALUES (?, ?, ?) RETURNING *";

    @Override
    public Account selectAccountById(String id) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    return mapAccount(resultSet);
            }
            return null;
        } catch (SQLException e) {
            throw databaseError("Could not select account.", e);
        }
    }

    @Override
    public Account insertAccount(String firstName, String lastName, String pin) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, pin);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return mapAccount(resultSet);
            }
        } catch (SQLException e) {
            throw databaseError("Could not insert account.", e);
        }
    }

    private Account mapAccount(ResultSet resultSet) throws SQLException {
        return new Account(
                resultSet.getString("account_id"),
                resultSet.getString("account_type"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("pin"),
                resultSet.getLong("balance"),
                resultSet.getString("created_at"));
    }

    private IllegalStateException databaseError(String message, SQLException cause) {
        return new IllegalStateException(message, cause);
    }

}
