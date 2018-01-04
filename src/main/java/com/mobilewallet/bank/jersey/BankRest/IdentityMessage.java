/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.mobilewallet.bank.jersey.BankRest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * IdentityMessage for converting Rest messages to java objects
 * @author Martin Stepanek
 */
@XmlRootElement
public class IdentityMessage {

    public static String TRANSACTION_ACTION = "transaction";
    public static String BALANCE_ACTION = "balance";
    public static String HISTORY_ACTION = "history";

    private String accountNumber;
    private String action;
    private String mqttMessage;

    public IdentityMessage() {
    }

    public IdentityMessage(String accountNumber, String action) {
        this.accountNumber = accountNumber;
        this.action = action;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMqttMessage() {
        return mqttMessage;
    }

    public void setMqttMessage(String mqttMessage) {
        this.mqttMessage = mqttMessage;
    }
}
