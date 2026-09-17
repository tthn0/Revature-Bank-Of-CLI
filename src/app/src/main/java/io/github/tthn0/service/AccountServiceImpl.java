package io.github.tthn0.service;

import io.github.tthn0.domain.Account;
import io.github.tthn0.persistence.AccountDao;

public class AccountServiceImpl implements AccountService {
    private final AccountDao accountDao;

    public AccountServiceImpl(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public Account createAccount(String firstName, String lastName, String pin) {
        return accountDao.insertAccount(firstName, lastName, pin);
    }

    @Override
    public Account login(String accountId, String pin) {
        Account account = accountDao.selectAccountById(accountId);
        if (account == null)
            return null;
        return account.isPinCorrect(pin) ? account : null;
    }

    @Override
    public Account findAccount(String accountId) {
        return accountDao.selectAccountById(accountId);
    }
}
