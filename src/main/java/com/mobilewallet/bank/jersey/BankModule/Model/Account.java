/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.mobilewallet.bank.jersey.BankModule.Model;

import java.util.Date;

/**
 * Account java class from database
 * @author Martin Stepanek
 */
public class Account {
    private int id;
    private int bankId;
    private String userId;
    private String accountNumber;
    private double balance;
    private Date dateCreated;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBankId() {
        return bankId;
    }

    public void setBankId(int bankId) {
        this.bankId = bankId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
}
