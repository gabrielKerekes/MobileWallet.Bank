/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.smartwallet.bank.jersey.BankRest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This subclass defines destination accounts with amount in transaction
 */
@XmlRootElement
public class Destination {
    private String destinationAccount;
    private double amount;

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}