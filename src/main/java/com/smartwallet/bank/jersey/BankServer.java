/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.smartwallet.bank.jersey;

import com.smartwallet.bank.jersey.BankModule.BankApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

/**
  * Defines the main class of Bank server application
  * @author Martin Stepanek
  */

 @ApplicationPath("/")
public class BankServer extends Application {

     private final Logger logger = LoggerFactory.getLogger(BankServer.class);

    /**
     * Constructor of Bank server
     * Starts new instance of Bank application and start all bank clients.
     */
    public BankServer() {
        super();

        BankApp bank = BankApp.getInstance();
        String status = bank.startClients();

        // Uncomment for testing purposes
        //bank.runTestClient("userTest", "0200", "SK2402000000001234567890");

        logger.info(status);
    }

    @Override
    public Set<Object> getSingletons() {
        return super.getSingletons();
    }

}
