package io.github.tthn0.app;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import io.github.tthn0.domain.Account;
import io.github.tthn0.domain.Audit;
import io.github.tthn0.service.AccountService;
import io.github.tthn0.service.LedgerException;
import io.github.tthn0.service.LedgerService;

public class Repl {
    private final AccountService as;
    private final LedgerService ls;
    private final Scanner scan = new Scanner(System.in);
    private static final Logger logger = LoggerFactory.getLogger(Repl.class);

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";

    private String getInput(String prefix) {
        System.out.print(GREEN + prefix);
        String input = scan.nextLine().trim();
        System.out.print(RESET);
        return input;
    }

    private String getInput() {
        return getInput("");
    }

    private void printError(String message) {
        System.out.print(RED);
        System.out.println(message);
        System.out.print(RESET);
    }

    @FunctionalInterface
    private interface ThrowableAction {
        void execute() throws Exception;
    }

    private record Command(String description, ThrowableAction action) {
    }

    public Repl(AccountService as, LedgerService ls) {
        this.as = as;
        this.ls = ls;
    }

    public void run() {
        System.out.println("\nWelcome to the Bank of CLI!");
        while (true) {
            boolean shouldExit = showMenu("Please choose an option below:", getMainMenu());
            if (shouldExit)
                break;
        }
    }

    private boolean showMenu(String header, Map<String, Command> commands) {
        System.out.println();
        System.out.println(header);
        commands.forEach((key, cmd) -> System.out.println("  " + key + ". " + cmd.description()));

        System.out.print("\n> ");
        String option = getInput();
        System.out.println();

        Command command = commands.get(option);
        if (command == null) {
            printError("Invalid option.");
        } else {
            try {
                command.action().execute();
            } catch (LedgerException e) {
                printError("\n" + e.getMessage());
            } catch (Exception e) {
                printError("\nAn unexpected error occurred: " + e.getMessage());
                logger.error("{}", e.getMessage());
            }
        }

        return option.equals("0");
    }

    private Map<String, Command> getMainMenu() {
        String goodbyeMessage = "Thank you for doing business with the Bank of CLI. Come back soon!";
        Map<String, Command> menu = new LinkedHashMap<>();
        menu.put("0", new Command("Exit", () -> System.out.println(goodbyeMessage)));
        menu.put("1", new Command("Login", this::handleLogin));
        menu.put("2", new Command("Create Account", this::handleCreateAccount));
        return menu;
    }

    private void handleLogin() {
        Account account = null;
        System.out.println("Please enter your credentials below:");

        while (account == null) {
            System.out.print("Account ID: ");
            String accountId = getInput();
            System.out.print("PIN: ");
            String pin = getInput();
            account = as.login(accountId, pin);

            if (account == null)
                printError("\nInvalid credentials. Please try again.\n");
        }

        boolean loggedIn = true;
        while (loggedIn) {
            String header = String.format("Welcome back, %s. Please select an option below:", account.getFirstName());
            loggedIn = !showMenu(header, getAccountMenu(account));
        }
    }

    private Map<String, Command> getAccountMenu(Account account) {
        Map<String, Command> menu = new LinkedHashMap<>();
        menu.put("0", new Command("Log Out", () -> handleLogOut()));
        menu.put("1", new Command("Deposit", () -> handleDeposit(account)));
        menu.put("2", new Command("Withdraw", () -> handleWithdraw(account)));
        menu.put("3", new Command("Transfer", () -> handleTransfer(account)));
        menu.put("4", new Command("Check Balance", () -> handleCheckBalance(account)));
        menu.put("5", new Command("View Transaction History", () -> handleViewHistory(account)));
        return menu;
    }

    private void handleLogOut() {
        System.out.println("Logging out...");
    }

    private void handleCreateAccount() {
        System.out.println("Please enter your account details below:");

        System.out.print("First Name: ");
        String firstName = getInput();
        System.out.print("Last Name: ");
        String lastName = getInput();
        System.out.print("PIN: ");
        String pin = getInput();

        if (pin.length() < 4) {
            printError(String.format("\nPIN must be at least 4 characters."));
            return;
        }

        Account account = as.createAccount(firstName, lastName, pin);
        System.out.printf("%nAccount successfully created! Your account ID is: %s.%n", account.getAccountId());
        System.out.println("You can now sign in using option (1) in the main menu.");
    }

    private long promptForAmount(String promptMessage) {
        long amountInCents = -1;
        while (amountInCents < 0) {
            System.out.print(promptMessage);
            String input = getInput("$");

            // ^\d+ -> Starts with one or more digits (whole dollars)
            // (\.\d{2})? -> Optionally followed by a decimal point and exactly two digits
            if (!input.matches("^\\d+(\\.\\d{2})?$")) {
                printError("\nInvalid format. Enter a whole amount, optionally with a decimal and two cent digits.\n");
                continue;
            }

            try {
                if (input.contains(".")) {
                    String[] parts = input.split("\\.");
                    long dollars = Long.parseLong(parts[0]);
                    long cents = Long.parseLong(parts[1]);
                    amountInCents = (dollars * 100) + cents;
                } else {
                    amountInCents = Long.parseLong(input) * 100;
                }
            } catch (NumberFormatException e) {
                printError("\nInvalid number format. Please try again.\n");
                amountInCents = -1;
            }
        }
        return amountInCents;
    }

    private String promptForMemo() {
        System.out.print("Memo: ");
        String memo = getInput();
        return memo.isBlank() ? "-" : memo;
    }

    private void handleDeposit(Account account) throws LedgerException {
        long amountInCents = promptForAmount("Amount to deposit: ");
        String memo = promptForMemo();
        String transactionId = ls.deposit(account, amountInCents, memo);

        printLoadingIndicator();

        System.out.println("\nDeposit successful! Your transaction ID is:");
        System.out.println(transactionId);
    }

    private void handleWithdraw(Account account) throws LedgerException {
        long amountInCents = promptForAmount("Amount to withdraw: ");
        String memo = promptForMemo();
        String transactionId = ls.withdraw(account, amountInCents, memo);

        printLoadingIndicator();

        System.out.println("\nWithdrawal successful! Your transaction ID is:");
        System.out.println(transactionId);
    }

    private void handleTransfer(Account fromAccount) throws LedgerException {
        System.out.print("Enter the account ID you'd like to transfer to: ");
        String toAccountId = getInput();

        Account toAccount = as.findAccount(toAccountId);
        if (toAccount == null) {
            printError(String.format("\nAccount ID %s could not be found.", toAccountId));
            return;
        } else if (fromAccount.getAccountId().equals(toAccount.getAccountId())) {
            printError(String.format("\nYou cannot transfer to yourself.", toAccountId));
            return;
        }

        long amountInCents = promptForAmount("Amount to transfer: ");
        String memo = promptForMemo();
        String transactionId = ls.transfer(fromAccount, toAccount, amountInCents, memo);

        printLoadingIndicator();

        System.out.println("\nTransfer successful! Your transaction ID is:");
        System.out.println(transactionId);
    }

    private void handleCheckBalance(Account account) {
        Account freshAccount = as.findAccount(account.getAccountId());
        account.setBalance(freshAccount.getBalanceInCents());

        AsciiTable at = new AsciiTable();
        at.getRenderer().setCWC(new CWC_LongestLine());

        at.addRule();
        at.addRow("Account Holder", "Account ID", "Balance");
        at.addRule();

        at.addRow(
            account.getFullName(),
            account.getAccountId(),
            account.getFormattedBalance());
        at.addRule();

        System.out.println(at.render());
    }

    private void handleViewHistory(Account account) {
        List<Audit> history = ls.getTransactionHistory(account);
        if (history.isEmpty()) {
            System.out.println("You have no transactions.");
            return;
        }

        AsciiTable at = new AsciiTable();
        at.getRenderer().setCWC(new CWC_LongestLine());

        at.addRule();
        at.addRow("Type", "Direction", "Amount", "Description", "Datetime");
        at.addRule();

        history.forEach(audit -> {
            at.addRow(
                    audit.getTransactionType(),
                    audit.getDirection(),
                    audit.getFormattedAmount(),
                    audit.getDescription(),
                    audit.getCreatedAt());
            at.addRule();
        });

        System.out.println(at.render());
    }

    private void printLoadingIndicator() {
        System.out.println();
        try {
            for (int i = 0; i < 3; i++) {
                System.out.print(".");
                Thread.sleep(250);
            }
        } catch (InterruptedException e) {
        }
        System.out.println();
    }
}