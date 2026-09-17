package io.github.tthn0.persistence;

import java.util.List;

import io.github.tthn0.domain.Audit;

public interface LedgerDao {
    List<Audit> selectAuditsFromAccountId(String accountId);

    String callDeposit(String accountId, long amountInCents, String memo);

    String callWithdraw(String accountId, long amountInCents, String memo);

    String callTransfer(String from, String to, long amountInCents, String memo);
}
