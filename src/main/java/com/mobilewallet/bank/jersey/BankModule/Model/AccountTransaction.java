/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.mobilewallet.bank.jersey.BankModule.Model;

/**
 * AccountTransaction java class from database
 * @author Martin Stepanek
 */
public class AccountTransaction {
    private int id;
    private int toId;
    private int transactionId;
    private double amount;
    private String message;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getToId() {
        return toId;
    }

    public void setToId(int toId) {
        this.toId = toId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
