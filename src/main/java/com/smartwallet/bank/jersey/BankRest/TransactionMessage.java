/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.smartwallet.bank.jersey.BankRest;

import com.smartwallet.bank.jersey.BankModule.Manager.DatabaseManager;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * TransactionMessage for converting Rest messages to java objects
 * @author Martin Stepanek
 */
@XmlRootElement
public class TransactionMessage {

    public static String EXPIRED = "EXPIRED";
    public static String CONFIRMED = "CONFIRMED";
    public static String REJECTED = "REJECTED";
    public static String ERROR = "ERROR";

    private String paymentId;
    private String status;

    public TransactionMessage() {
    }

    public TransactionMessage(String paymentId, String status) {
        this.paymentId = paymentId;
        this.status = status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Change status message from REST to DB format
     * @return status as it is in DB
     */
    public String getStatusForDB() {
        String status = "";

        if(this.status.equals(EXPIRED)) {
            status = DatabaseManager.EXPIRED;
        }
        else if(this.status.equals(CONFIRMED)) {
            status = DatabaseManager.RECEIVED;
        }
        else if(this.status.equals(EXPIRED)) {
            status = DatabaseManager.EXPIRED;
        }
        else if(this.status.equals(REJECTED) || this.status.equals(ERROR)) {
            status = DatabaseManager.REJECTED;
        }

        return status;
    }
}
