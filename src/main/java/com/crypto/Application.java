package com.crypto;

import com.crypto.slack.StatusChecker;

public class Application {

    public static void main(String[] args) {
        StatusChecker checker = new StatusChecker();
        checker.checkStatus();
    }
}