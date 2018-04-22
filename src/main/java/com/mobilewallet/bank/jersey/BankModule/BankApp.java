/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.mobilewallet.bank.jersey.BankModule;

import com.mobilewallet.bank.jersey.BankModule.Model.Bank;
import com.mobilewallet.bank.jersey.BankModule.Test.Client;
import com.mobilewallet.bank.jersey.BankModule.Manager.ConfigManager;
import com.mobilewallet.bank.jersey.BankModule.Manager.DatabaseManager;
import com.mobilewallet.bank.jersey.HttpsCertificateUtils;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Defines the BankApp class that instantiates all bank clients and handle connections to broker
 * @author Martin Stepanek
 */
public class BankApp {

    private static BankApp instance = null;
    private ConfigManager cm;
    private static DatabaseManager dm;
    private final Logger logger = LoggerFactory.getLogger(BankApp.class);

    // list of banks stored in DB and bank clients
    private ArrayList<Bank> banks = new ArrayList<>();
    private ArrayList<BankClient> bankClients = new ArrayList<>();

    /**
     * Client used for test purposes
     */
    private Client testClient;


    /**
     * BankApp singleton
     * Set all banks from database to array
     */
    private BankApp() {
        dm = DatabaseManager.getInstance();
        cm = ConfigManager.getInstance();
        try {
            banks = dm.getAllBanks();
        } catch (SQLException e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }
    }


    /**
     * BankApp constructor
     * @return instance of BankApp
     */
    public static BankApp getInstance() {
        if(instance == null) {
            instance = new BankApp();
        }

        return instance;
    }


    /**
     * Get all bankClients
     * @return ArrayList of BankClient
     */
    public ArrayList<BankClient> getBankClients() {
        return bankClients;
    }


    /**
     * Set BankClients
     * @param bankClients array of BankCLient
     */
    public void setBankClients(ArrayList<BankClient> bankClients) {
        this.bankClients = bankClients;
    }


    /**
     * Get TestClient
     * @return Client object
     */
    public Client getTestClient() {
        return testClient;
    }


    /**
     * Set testClient
     * @param testClient Client
     */
    public void setTestClient(Client testClient) {
        this.testClient = testClient;
    }

    /**
     * Method connect all clients that are stored in database
     * @return status message
     */
    public String startClients() {

        String status = "Bank clients connected";

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        try {
            connOpts.setSocketFactory(HttpsCertificateUtils.getSslContextWithTrustedCertificate().getSocketFactory());
        } catch (Exception e) {
            logger.error("Error while setting secure socket factory: ", e);
            e.printStackTrace();
        }

        try {
            for(Bank bank : banks) {
                BankClient bankClient = new BankClient(cm.getPropValues("BROKER"), bank.getShortName(), bank.getBic(), new MemoryPersistence());

                bankClient.connect(connOpts);
                bankClient.subscribeTopics();

                logger.info(bank.getShortName() + " is connected");
                bankClients.add(bankClient);
            }
        } catch (MqttException e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }

        return status;
    }


    /**
     * Method for disconnecting all bank clients
     * @return status message
     */
    public String endClients() {
        String status = "Bank clients disconnected";
        try {
            for(BankClient bankClient : bankClients) {
                bankClient.disconnect();
                logger.info(bankClient.getClientId() + " is disconnected");
            }
        } catch (MqttException e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }

        return status;
    }


    /**
     * Method will create test client, simulates user
     * @param name Name of the user
     * @param bic Bank identification code same as in accountNumber
     * @param accountNumber String, account number as saved in database
     * @throws MqttException on Mqtt error
     */
    public void runTestClient(String name, String bic, String accountNumber) {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        String broker = cm.getPropValues("BROKER");

        try {
            testClient = new Client(broker, name, bic, accountNumber, new MemoryPersistence());
            testClient.connect(connOpts);

            testClient.subscribe(BankClient.BANK_TOPIC + "/response/balance/" + bic + "/" + accountNumber);
            testClient.subscribe(BankClient.BANK_TOPIC + "/response/history/" + bic + "/" + accountNumber);
            testClient.subscribe(BankClient.PAYMENT_ORDER_RESPONSES);
        } catch (MqttException e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }

    }

    /**
     * Check connection of bank, by String id
     * @param bankId id of bank, String
     * @return connection Status, boolean
     */
    public boolean isConnected(String bankId) {

        boolean connectionStatus = false;
        for (BankClient bankClient : bankClients) {
            if(bankClient.getClientId().equals(bankId)) {
                connectionStatus = bankClient.isConnected();
            }
        }

        return connectionStatus;
    }

}
