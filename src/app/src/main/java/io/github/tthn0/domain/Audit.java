package io.github.tthn0.domain;

public class Audit {
    private final String accountId;
    private final String accountHolder;
    private final String transactionType;
    private final String direction;
    private final long amountInCents;
    private final String description;
    private final String createdAt;

    public Audit(
            String accountId,
            String accountHolder,
            String transactionType,
            String direction,
            long amountInCents,
            String description,
            String createdAt) {
        this.accountId = accountId;
        this.accountHolder = accountHolder;
        this.transactionType = transactionType;
        this.direction = direction;
        this.amountInCents = amountInCents;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getTransactionType() {
        return this.transactionType;
    }

    public String getDirection() {
        return this.direction;
    }

    public String getFormattedAmount() {
        return String.format("$%(,.2f", this.amountInCents / 100.0);
    }

    public String getDescription() {
        return this.description;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public String toString() {
        return String.format(
                "Audit(%s, %s, %s, %s, %s)",
                transactionType,
                direction,
                getFormattedAmount(),
                description,
                createdAt);
    }
}
