package io.github.tthn0.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import io.github.tthn0.persistence.AccountDao;
import io.github.tthn0.persistence.AccountDaoImpl;
import io.github.tthn0.persistence.LedgerDao;
import io.github.tthn0.persistence.LedgerDaoImpl;
import io.github.tthn0.service.AccountService;
import io.github.tthn0.service.AccountServiceImpl;
import io.github.tthn0.service.LedgerService;
import io.github.tthn0.service.LedgerServiceImpl;

public class Main {
    public static void main(String[] args) {
        AccountDao ad = new AccountDaoImpl();
        LedgerDao ld = new LedgerDaoImpl();
        AccountService as = new AccountServiceImpl(ad);
        LedgerService ls = new LedgerServiceImpl(ad, ld);
        printBanner();
        new Repl(as, ls).run();
    }

    private static void printBanner() {
        String filePath = "src/main/resources/banner.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null)
                System.out.println(line);
        } catch (IOException e) {
            System.err.println("Can't find file: " + new File(filePath).getAbsolutePath());
        }
    }
}