package io.github.tthn0.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.tthn0.domain.Account;
import io.github.tthn0.persistence.AccountDao;

public class AccountServiceImplTest {
    private AccountDao dao;
    private AccountService service;

    private String accountId;
    private String correctPin;
    private Account account;

    @BeforeEach
    void setUp() {
        this.dao = mock(AccountDao.class);
        this.service = new AccountServiceImpl(this.dao);
        this.accountId = "ACT-0000";
        this.correctPin = "0000";
        this.account = new Account(accountId, "CUSTOMER", "First", "Last", correctPin, 0, "");
    }

    @Test
    void testLogin() {
        when(dao.selectAccountById(this.accountId)).thenReturn(this.account);

        Account successfulLogin = service.login(accountId, correctPin);
        Account unsuccessfulLogin = service.login(accountId, "Wrong Pin");

        assertNotNull(successfulLogin);
        assertNull(unsuccessfulLogin);
    }

    @Test
    void testFindAccount() {
        when(dao.selectAccountById(this.accountId)).thenReturn(this.account);

        Account existingAccount = service.findAccount(this.accountId);
        Account nonexistingAccount = service.findAccount("Non Existent");

        assertNotNull(existingAccount);
        assertNull(nonexistingAccount);
    }
}
