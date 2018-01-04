/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.mobilewallet.bank.jersey.BankModule.Test;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client used for testing purposes
 * Class will instantiate Mqtt client
 * @author Martin Stepanek
 */
public class Client extends MqttClient implements MqttCallback {

    //Quality of service
    private final int qos             = 2;

    private String clientId;
    private String topic;
    private String bic;
    private String accountNumber;

    private MemoryPersistence persistence = new MemoryPersistence();
    private MqttConnectOptions connectionOptions;

    private final Logger logger = LoggerFactory.getLogger(Client.class);

    public Client(String serverURI, String clientId) throws MqttException {
        super(serverURI, clientId);
        this.clientId = clientId;
    }

    public Client(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
        super(serverURI, clientId, persistence);
        this.clientId = clientId;
        setCallback(this);
    }

    public Client(String serverURI, String clientId, String bic, String accountNumber, MqttClientPersistence persistence) throws MqttException {
        super(serverURI, clientId, persistence);
        this.clientId = clientId;
        this.bic = bic;
        this.accountNumber = accountNumber;
        setCallback(this);
    }

    public MqttMessage setMessage(String content) {
        MqttMessage msg = new MqttMessage(content.getBytes());
        msg.setQos(qos);

        return msg;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public MqttConnectOptions getConnectionOptions() {
        return connectionOptions;
    }

    public void setConnectionOptions(MqttConnectOptions connectionOptions) {
        this.connectionOptions = connectionOptions;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String s, MqttMessage message) throws Exception {
        logger.debug(clientId + ": Delivered message is '" + message + "' from " + s);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        System.out.println("Delivery completed");
    }
}
