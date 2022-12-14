package com.example.application;

import com.example.application.model.Test;
import com.example.application.model.TestStatus;
import com.example.application.tests.InvoicesDownloadTest;

import java.time.LocalDate;

public class ThreadTest extends Thread {

    private final Test test;
    private final LocalDate date;

    public ThreadTest(Test test, LocalDate date) {
        this.test = test;
        this.date = date;
    }

    public void run() {
        InvoicesDownloadTest invoicesDownloadTest = new InvoicesDownloadTest();
        System.out.println("Uruchomiono test:" + this.test.getName());
        this.test.setStatus(TestStatus.progress);
        switch (test.getName().toLowerCase()) {
            case "pko": {
                invoicesDownloadTest.pko();
                break;
            }
            case "leaselink":{
                invoicesDownloadTest.leaseLink();
                break;
            }
            case "t-mobile":{
                invoicesDownloadTest.tMobile();
                break;
            }
            case "toyota":{
                invoicesDownloadTest.toyota();
                break;
            }
            case "fakturownia":{
                invoicesDownloadTest.fakturownia(date);
                break;
            }
        }
        System.out.println("Zakończono test: " + this.test.getName());
    }
}

