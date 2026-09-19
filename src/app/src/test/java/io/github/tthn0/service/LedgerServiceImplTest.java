package io.github.tthn0.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.tthn0.domain.Account;
import io.github.tthn0.persistence.AccountDao;
import io.github.tthn0.persistence.LedgerDao;

public class LedgerServiceImplTest {
    private AccountDao accountDao;
    private LedgerDao ledgerDao;
    private LedgerService ledgerService;

    private String accountId1;
    private String pin1;
    private Account account1;

    private String accountId2;
    private String pin2;
    private Account account2;

    @BeforeEach
    void setUp() {
        this.accountDao = mock(AccountDao.class);
        this.ledgerDao = mock(LedgerDao.class);
        this.ledgerService = new LedgerServiceImpl(this.accountDao, this.ledgerDao);

        this.accountId1 = "ACT-1111";
        this.pin1 = "1111";
        this.account1 = new Account(accountId1, "CUSTOMER", "Account", "One", pin1, 0, "");

        this.accountId2 = "ACT-2222";
        this.pin2 = "2222";
        this.account2 = new Account(accountId2, "CUSTOMER", "Account", "Two", pin2, 0, "");
    }

    @Test
    void testDepositingNegativeAmountThrowsError() {
        long negativeAmount = -100;
        LedgerException exception = assertThrows(
                LedgerException.class,
                () -> this.ledgerService.deposit(account1, negativeAmount, "Negative"));
        assertEquals("You can only deposit a positive amount.", exception.getMessage());
    }

    @Test
    void testSuccessfulDeposit() {
        long amountInCents = 100;
        String memo = "Positive";

        assertDoesNotThrow(() -> this.ledgerService.deposit(account1, amountInCents, memo));
        verify(ledgerDao, times(1)).callDeposit(accountId1, amountInCents, memo);
        assertEquals(account1.getBalanceInCents(), amountInCents);
    }

    @Test
    void testWithdrawingNegativeAmountThrows() {
        long negativeAmount = -100;
        LedgerException exception = assertThrows(
                LedgerException.class,
                () -> this.ledgerService.withdraw(account1, negativeAmount, "Negative"));
        assertEquals("You can only withdraw a positive amount.", exception.getMessage());
    }

    @Test
    void testWithdrawingMoreThanYouHaveThrows() {
        long amountToWithdraw = 100;
        LedgerException exception = assertThrows(
                LedgerException.class,
                () -> this.ledgerService.withdraw(account1, amountToWithdraw, "Not enough money"));
        assertEquals(String.format("Insufficient funds. You only have %s in your account.",
                this.account1.getFormattedBalance()), exception.getMessage());
    }

    @Test
    void testSuccessfulWithdraw() {
        long startingBalance = 500;
        long amountToWithdraw = 100;
        this.account1.updateBalance(startingBalance);
        String memo = "Success";

        assertDoesNotThrow(() -> this.ledgerService.withdraw(account1, amountToWithdraw, memo));
        verify(ledgerDao, times(1)).callWithdraw(accountId1, amountToWithdraw, memo);
        assertEquals(account1.getBalanceInCents(), startingBalance - amountToWithdraw);
    }

    @Test
    void testTransferringNegativeAmountThrows() {
        long negativeAmount = -100;
        LedgerException exception = assertThrows(
                LedgerException.class,
                () -> this.ledgerService.transfer(account1, account2, negativeAmount, "Negative"));
        assertEquals("You can only transfer a positive amount.", exception.getMessage());
    }

    @Test
    void testTransferringMoreThanYouHaveThrows() {
        long amountToTransfer = 100;
        LedgerException exception = assertThrows(
                LedgerException.class,
                () -> this.ledgerService.transfer(account1, account2, amountToTransfer, "Not enough money"));
        assertEquals(String.format("Insufficient funds. You only have %s in your account.",
                this.account1.getFormattedBalance()), exception.getMessage());

    }

    @Test
    void testTransferringToInvalidAccountThrows() {
        long startingBalance = 1000;
        account1.updateBalance(startingBalance);

        String invalidAccountId = "INVALID";
        Account invalidAccount = new Account(invalidAccountId, "CUSTOMER", "First", "Last", "PIN", 0, "");

        long amountToTransfer = 500;
        when(this.accountDao.selectAccountById(invalidAccountId)).thenReturn(null);
        LedgerException exception = assertThrows(
                LedgerException.class,
                () -> this.ledgerService.transfer(account1, invalidAccount, amountToTransfer, "Invalid Account"));
        assertEquals("Receiving account could not be found.", exception.getMessage());
    }

    @Test
    void testSuccessfulTransfer() {
        long startingBalance = 1000;
        account1.updateBalance(startingBalance);

        long amountToTransfer = 500;
        String memo = "Successful Transfer";
        when(this.accountDao.selectAccountById(accountId2)).thenReturn(account2);

        assertDoesNotThrow(() -> this.ledgerService.transfer(account1, account2, amountToTransfer, memo));
        verify(ledgerDao, times(1)).callTransfer(accountId1, accountId2, amountToTransfer, memo);

        assertEquals(account1.getBalanceInCents(), startingBalance - amountToTransfer);
        assertEquals(account2.getBalanceInCents(), amountToTransfer);
    }
}
