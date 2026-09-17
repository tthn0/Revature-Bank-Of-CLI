package io.github.tthn0.service;

import io.github.tthn0.domain.Account;

public interface AccountService {
    Account createAccount(String firstName, String lastName, String pin);

    Account login(String accountId, String pin);

    Account findAccount(String accountId);
}
