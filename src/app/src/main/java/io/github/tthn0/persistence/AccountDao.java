package io.github.tthn0.persistence;

import io.github.tthn0.domain.Account;

public interface AccountDao {
    Account selectAccountById(String id);

    Account insertAccount(String firstName, String lastName, String pin);
}
