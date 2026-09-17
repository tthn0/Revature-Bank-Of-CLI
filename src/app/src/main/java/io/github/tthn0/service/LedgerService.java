package io.github.tthn0.service;

import java.util.List;

import io.github.tthn0.domain.Account;
import io.github.tthn0.domain.Audit;

public interface LedgerService {

    List<Audit> getTransactionHistory(Account account);

    String deposit(Account account, long amountInCents, String memo) throws LedgerException;

    String withdraw(Account account, long amountInCents, String memo) throws LedgerException;

    String transfer(Account from, Account to, long amountInCents, String memo) throws LedgerException;
}
