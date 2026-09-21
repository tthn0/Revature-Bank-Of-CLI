package io.github.tthn0.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.tthn0.domain.Account;
import io.github.tthn0.persistence.AccountDao;

public class AccountServiceImpl implements AccountService {
    private final AccountDao accountDao;
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

    public AccountServiceImpl(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public Account createAccount(String firstName, String lastName, String pin) {
        Account account = accountDao.insertAccount(firstName, lastName, pin);

        logger.info("\"Account created for {} {}.\"", firstName, lastName);
        return account;
    }

    @Override
    public Account login(String accountId, String pin) {
        Account account = accountDao.selectAccountById(accountId);
        boolean successfulLogin = account != null && account.isPinCorrect(pin);

        if (successfulLogin) {
            logger.info("\"{} logged in.\"", account.getAccountId());
            return account;
        } else {
            logger.warn("\"Failed login attempt for {}.\"", accountId);
            return null;
        }
    }

    @Override
    public Account findAccount(String accountId) {
        return accountDao.selectAccountById(accountId);
    }
}
