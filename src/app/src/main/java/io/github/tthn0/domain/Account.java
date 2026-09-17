package io.github.tthn0.domain;

public class Account {
    private final String accountId;
    private final String accountType;
    private final String firstName;
    private final String lastName;
    private final String pin;
    private long balanceInCents;
    private final String createdAt;

    public Account(
            String accountId,
            String accountType,
            String firstName,
            String lastName,
            String pin,
            long balanceInCents,
            String createdAt) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.firstName = firstName;
        this.lastName = lastName;
        this.pin = pin;
        this.balanceInCents = balanceInCents;
        this.createdAt = createdAt;
    }

    public String getFormattedBalance() {
        return String.format("$%,.2f", this.balanceInCents / 100.0);
    }

    @Override
    public String toString() {
        return String.format("Account(%s, %s %s, %s)", accountId, firstName, lastName, getFormattedBalance());
    }

    public boolean isPinCorrect(String pin) {
        return this.pin.equals(pin);
    }

    public String getAccountId() {
        return this.accountId;
    }

    public long getBalanceInCents() {
        return this.balanceInCents;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void updateBalance(long amountInCents) {
        this.balanceInCents += amountInCents;
    }
}
