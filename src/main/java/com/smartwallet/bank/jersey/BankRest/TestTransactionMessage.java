/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.smartwallet.bank.jersey.BankRest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;

/**
 * This class is used to define Mqtt message used for testing transactions
 *
 * @author Martin Stepanek
 */
@XmlRootElement
@XmlSeeAlso({Destination.class})
public class TestTransactionMessage {

    private String paymentId;
    private String currency;
    private String message;

    @XmlElement
    private ArrayList<Destination> destinations;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ArrayList<Destination> getDestinations() {
        return destinations;
    }

    public void setDestinations(ArrayList<Destination> destinations) {
        this.destinations = destinations;
    }
}
