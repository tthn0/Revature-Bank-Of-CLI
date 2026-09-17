package io.github.tthn0.persistence;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import io.github.tthn0.domain.Audit;

public class LedgerDaoImpl implements LedgerDao {
    private static final String SELECT_SQL = "SELECT * FROM audits WHERE account_id = ?";
    private static final String DEPOSIT_SQL = "{ ? = call deposit(?, ?, ?) }";
    private static final String WITHDRAW_SQL = "{ ? = call withdraw(?, ?, ?) }";
    private static final String TRANSFER_SQL = "{ ? = call transfer(?, ?, ?, ?) }";

    @Override
    public List<Audit> selectAuditsFromAccountId(String accountId) {
        List<Audit> audits = new ArrayList<>();
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_SQL)) {
            statement.setString(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    audits.add(mapAudit(resultSet));
            }
            return audits;
        } catch (SQLException e) {
            throw databaseError("Could not select audits.", e);
        }
    }

    private String callUdfHelper(String sql, String errorMessage, String accountId, long amountInCents, String memo) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection();
                CallableStatement statement = connection.prepareCall(sql)) {
            statement.registerOutParameter(1, Types.OTHER);
            statement.setString(2, accountId);
            statement.setLong(3, amountInCents);
            statement.setString(4, memo);
            statement.execute();

            Object transactionId = statement.getObject(1);
            return transactionId.toString();
        } catch (SQLException e) {
            throw databaseError(errorMessage, e);
        }
    }

    @Override
    public String callDeposit(String accountId, long amountInCents, String memo) {
        return callUdfHelper(DEPOSIT_SQL, "Could not deposit.", accountId, amountInCents, memo);
    }

    @Override
    public String callWithdraw(String accountId, long amountInCents, String memo) {
        return callUdfHelper(WITHDRAW_SQL, "Could not withdraw.", accountId, amountInCents, memo);
    }

    @Override
    public String callTransfer(String fromAccountId, String toAccountId, long amountInCents, String memo) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection();
                CallableStatement statement = connection.prepareCall(TRANSFER_SQL)) {
            statement.registerOutParameter(1, Types.OTHER);
            statement.setString(2, fromAccountId);
            statement.setString(3, toAccountId);
            statement.setLong(4, amountInCents);
            statement.setString(5, memo);
            statement.execute();

            Object transactionId = statement.getObject(1);
            return transactionId.toString();
        } catch (SQLException e) {
            throw databaseError("Could not transfer.", e);
        }
    }

    private Audit mapAudit(ResultSet resultSet) throws SQLException {
        return new Audit(
                resultSet.getString("account_id"),
                resultSet.getString("account_holder"),
                resultSet.getString("transaction_type"),
                resultSet.getString("direction"),
                resultSet.getLong("amount"),
                resultSet.getString("description"),
                resultSet.getString("created_at"));
    }

    private IllegalStateException databaseError(String message, SQLException cause) {
        return new IllegalStateException(message, cause);
    }
}
