/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.smartwallet.bank.jersey.BankRest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * AccountMessage for converting Rest messages to java objects
 * Used when account is added
 * @author Martin Stepanek
 */
@XmlRootElement
public class AccountMessage {

    private String accountNumber;

    public AccountMessage() {
    }

    public AccountMessage(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

}
