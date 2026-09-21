package io.github.tthn0.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.tthn0.domain.Account;
import io.github.tthn0.domain.Audit;
import io.github.tthn0.persistence.AccountDao;
import io.github.tthn0.persistence.LedgerDao;

public class LedgerServiceImpl implements LedgerService {
    private final AccountDao accountDao;
    private final LedgerDao ledgerDao;
    private static final Logger logger = LoggerFactory.getLogger(LedgerServiceImpl.class);

    public LedgerServiceImpl(AccountDao accountDao, LedgerDao ledgerDao) {
        this.accountDao = accountDao;
        this.ledgerDao = ledgerDao;
    }

    @Override
    public List<Audit> getTransactionHistory(Account account) {
        List<Audit> transactionHistory = ledgerDao.selectAuditsFromAccountId(account.getAccountId());

        logger.info("\"{} viewed their transaction history.\"", account.getAccountId());
        return transactionHistory;
    }

    @Override
    public String deposit(Account account, long amountInCents, String memo) throws LedgerException {
        if (amountInCents <= 0)
            throw new LedgerException("You can only deposit a positive amount.");

        String transactionId = ledgerDao.callDeposit(account.getAccountId(), amountInCents, memo);
        account.updateBalance(amountInCents);

        logger.info(
                "\"(Transaction ID: {}) {} deposited {}¢.\"",
                transactionId,
                account.getAccountId(),
                amountInCents);
        return transactionId;
    }

    @Override
    public String withdraw(Account account, long amountInCents, String memo) throws LedgerException {
        if (amountInCents <= 0)
            throw new LedgerException("You can only withdraw a positive amount.");
        else if (amountInCents > account.getBalanceInCents())
            throw new LedgerException(String.format("Insufficient funds. You only have $%,.2f in your account.",
                    account.getBalanceInCents() / 100.0));

        String transactionId = ledgerDao.callWithdraw(account.getAccountId(), amountInCents, memo);
        account.updateBalance(-amountInCents);

        logger.info(
                "\"(Transaction ID: {}) {} withdrew {}¢.\"",
                transactionId,
                account.getAccountId(),
                amountInCents);
        return transactionId;
    }

    @Override
    public String transfer(Account from, Account to, long amountInCents, String memo) throws LedgerException {
        if (amountInCents <= 0)
            throw new LedgerException("You can only transfer a positive amount.");
        else if (amountInCents > from.getBalanceInCents())
            throw new LedgerException(String.format("Insufficient funds. You only have $%,.2f in your account.",
                    from.getBalanceInCents() / 100.0));
        else if (accountDao.selectAccountById(to.getAccountId()) == null)
            throw new LedgerException("Receiving account could not be found.");

        String transactionId = ledgerDao.callTransfer(from.getAccountId(), to.getAccountId(), amountInCents, memo);
        from.updateBalance(-amountInCents);
        to.updateBalance(+amountInCents);

        logger.info(
                "\"(Transaction ID: {}) {} transferred {}¢ to {}.\"",
                transactionId,
                from.getAccountId(),
                amountInCents,
                to.getAccountId());
        return transactionId;
    }
}
