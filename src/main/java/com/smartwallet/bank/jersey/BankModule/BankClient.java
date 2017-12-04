/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.smartwallet.bank.jersey.BankModule;

import com.smartwallet.bank.jersey.BankModule.Model.Account;
import com.smartwallet.bank.jersey.BankModule.Model.AccountTransaction;
import com.smartwallet.bank.jersey.BankModule.Model.Bank;
import com.smartwallet.bank.jersey.BankRest.IdentityMessage;
import org.apache.http.entity.StringEntity;
import com.smartwallet.bank.jersey.BankModule.Manager.ConfigManager;
import com.smartwallet.bank.jersey.BankModule.Manager.DatabaseManager;
import com.smartwallet.bank.jersey.BankModule.Model.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.ssl.SSLContexts;
import com.smartwallet.bank.jersey.BankModule.Model.Transaction;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.sql.Types.NULL;

/**
 * Defines the bank client and all logic in sending mqtt messages and sending HTTP requests
 * @author Martin Stepanek
 */
public class BankClient extends MqttClient implements MqttCallback {

    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String BANK_TOPIC = "/bank";
    public static final String PAYMENT_ORDERS = BANK_TOPIC + "/payment_orders";
    public static final String PAYMENT_ORDER_RESPONSES = BANK_TOPIC + "/payment_order_responses";
    public static final String REQUEST_BALANCE = BANK_TOPIC + "/request/balance";
    public static final String RESPONSE_BALANCE = BANK_TOPIC + "/response/balance";
    public static final String REQUEST_HISTORY = BANK_TOPIC + "/request/history";
    public static final String RESPONSE_HISTORY = BANK_TOPIC + "/response/history";
    public static final String REQUEST_BANK_ACCOUNT = BANK_TOPIC + "/request/link_account";

    private final Logger logger = LoggerFactory.getLogger(BankClient.class);

    private String clientId;
    private String bic;

    /**
     * Managers that is using
     */
    private DatabaseManager dm;
    private ConfigManager cm;

    /**
     * List of topics where the bank client subscribe
     */
    private String[] subscriptionTopics;

    /**
     * Variables that represents values from config
     */
    private int qos;
    private boolean isConfirmIdentity;
    private boolean isDevel;
    private String authUrl;
    private String transactionAuthUrl;
    private String linkAccountUrl;

    private CloseableHttpAsyncClient httpclient;


    /**
     * Constructor for bank client
     * Here constructor creates String for subscription topics
     * The constructor creates Trust strategy for validating self signed certificates on the server
     * @param serverURI server where is the MQTT broker
     * @param clientId String, id of client, that will be sent in mqtt messages
     * @param bic String, bank identification code
     * @param persistence MqttClientPersistence
     * @throws MqttException on Mqtt error
     */
    public BankClient(String serverURI, String clientId, String bic, MqttClientPersistence persistence) throws MqttException {
        super(serverURI, clientId, persistence);
        this.clientId = clientId;
        this.bic = bic;

        // Create all subscription topics
        subscriptionTopics = new String[]{REQUEST_BALANCE + "/" + bic + "/#",
                REQUEST_HISTORY + "/" + bic + "/#",
                REQUEST_BANK_ACCOUNT + "/" + bic + "/#",
                PAYMENT_ORDERS + "/" + bic + "/#"};

        dm = DatabaseManager.getInstance();
        cm = ConfigManager.getInstance();
        setCallback(this);

        // Get all needed values from config settings to local variables
        this.qos = Integer.parseInt(cm.getPropValues("QOS"));
        this.isDevel = Boolean.valueOf(cm.getPropValues("DEVELOPMENT"));
        this.isConfirmIdentity = Boolean.valueOf(cm.getPropValues("CONFIRM_IDENTITY"));
        this.authUrl = cm.getPropValues("AUTH_URL");
        this.transactionAuthUrl = cm.getPropValues("TRANSACTION_AUTH_URL");
        this.linkAccountUrl = cm.getPropValues("LINK_ACCOUNT_URL");

        // Set httpclient logger only to errors
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");


        // Create a trust manager that does not validate certificate chains
        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };

        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        } catch (Exception e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }

        httpclient = HttpAsyncClients.custom().setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).setSSLContext(sslContext).build();
    }

    /**
     * Set Mqtt message with content
     * @param content String, content of MqttMessage
     * @return MqttMessage
     */
    public MqttMessage setMessage(String content) {
        MqttMessage msg = new MqttMessage(content.getBytes());
        msg.setQos(qos);

        return msg;
    }

    /**
     * Subscribe all topics set by constructor
     * @throws MqttException on Mqtt error
     */
    public void subscribeTopics() throws MqttException {
        subscribe(subscriptionTopics);
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    /**
     * On Mqtt connection lost
     * @param throwable Mqtt error
     */
    @Override
    public void connectionLost(Throwable throwable) {
        logger.error("Bank client: " + this.bic + " lost connection with Mqtt broker.");
    }

    /**
     * On Mqtt message arrive
     * Decision logic what to do when given message arrives
     * @param s String, topic path
     * @param message MqttMessage, message arrived
     * @throws Exception on Exception
     */
    @Override
    public void messageArrived(String s, MqttMessage message) throws Exception {

        logger.debug(clientId + ": Delivered message is '" + message + "' from " + s);

        if(isConfirmIdentity) {
            doWithIdentityConfirmation(s, message);
        }
        else {
            doWithoutIdentityConfirmation(s, message);
        }
    }

    /**
     * On delivery complete
     * @param iMqttDeliveryToken Mqtt delivery token
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        logger.debug("Delivery completed");
    }

    /**
     * Decision logic for Mqtt messages if identity confirmation is required
     * @param s String, topic path
     * @param message Mqtt message
     * @throws Exception on exception
     */
    private void doWithIdentityConfirmation(String s, MqttMessage message) throws Exception {

        if(s.contains(PAYMENT_ORDERS) || ((s.contains(REQUEST_BALANCE) || s.contains(REQUEST_HISTORY)) && isJSONObject(message.toString()))) {
            if(s.contains(PAYMENT_ORDERS)) {
                JSONObject json = new JSONObject(message.toString());
                String accountNumber = json.getString("sourceAccount");
                // save requested status of transaction every time, if transaction was already processed don't do authentication request
                boolean status = publishPaymentStatus(message.toString(), DatabaseManager.REQUESTED);
                if(status) {
                    authRequest(accountNumber, "transaction", message);
                }
            }
            if(s.contains(REQUEST_BALANCE)) {
                JSONObject json = new JSONObject(message.toString());
                String accountNumber = json.getString("accountNumber");
                authRequest(accountNumber,"balance", message);
            }
            if(s.contains(REQUEST_HISTORY)) {
                JSONObject json = new JSONObject(message.toString());
                String accountNumber = json.getString("accountNumber");
                authRequest(accountNumber,"history", message);
            }
        }

    }

    /**
     * Decision logic for Mqtt messages if identity confirmation is NOT required
     * @param s String, topic path
     * @param message Mqtt message
     * @throws Exception on exception
     */
    private void doWithoutIdentityConfirmation(String s, MqttMessage message) throws Exception {

        if(s.contains(PAYMENT_ORDERS)) {
            paymentOrderRequest(message.toString());
        }
        if((s.contains(REQUEST_BALANCE) || s.contains(REQUEST_HISTORY)) && isJSONObject(message.toString())) {

            JSONObject json;
            json = new JSONObject(message.toString());

            if (s.contains(REQUEST_BALANCE) && json.has("accountNumber")) {
                try {
                    String accountNumber = json.getString("accountNumber");
                    getBalance(accountNumber);
                }
                catch(Exception e) {
                    logger.error("Error: ", e);
                    e.printStackTrace();
                }
            }
            if (s.contains(REQUEST_HISTORY) && json.has("accountNumber")) {
                try {
                    String accountNumber = json.getString("accountNumber");
                    getHistory(accountNumber);
                }
                catch(Exception e) {
                    logger.error("Error: ", e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Payment order request selected by paymentId
     * @param paymentId String, uuid of payment
     * @throws Exception on exception
     */
    public void paymentOrderRequestByPaymentId(String paymentId) throws Exception {
        Transaction t = dm.getTransactionByPaymentId(paymentId);
        String accountNumber = dm.getUserAccount(t.getFromId()).getAccountNumber();

        try {
            // send not enough money message if user has less money on account than the price of all items
            double balanceUser = dm.getUserAccountByNumber(accountNumber).getBalance();
            if(balanceUser < t.getAmount()) {
                logger.debug("Account number " + accountNumber + " has not enough money.");
                String timestamp = new SimpleDateFormat(TIME_FORMAT).format(Calendar.getInstance().getTime());
                dm.updateTransactionStatus(t.getId(),DatabaseManager.REJECTED, timestamp);
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("orderId", paymentId);
                jsonResponse.put("balance", balanceUser);
                jsonResponse.put("success", 0);
                jsonResponse.put("message", "Not enough money");
                this.publish(PAYMENT_ORDER_RESPONSES, this.setMessage(jsonResponse.toString()));
            } else {
                publishPaymentStatus(createMessageFromTransaction(t), DatabaseManager.PENDING);
                authTransactionRequest(accountNumber, paymentId, this.setMessage(createMessageFromTransaction(t)));
            }
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }
    }

    /**
     * Payment order request selected by message
     * @param message String, encoded json into String message
     * @throws Exception on exception
     */
    public void paymentOrderRequest(String message) throws Exception {
        JSONObject json = new JSONObject(message);
        String accountNumber = json.getString("sourceAccount");
        String paymentId = json.getString("paymentId");

        try {
            // send not enough money message if user has less money on account than the price of item is
            double balanceUser = dm.getUserAccountByNumber(accountNumber).getBalance();
            if(balanceUser < getTotalAmount(message)) {
                logger.debug("Account number " + accountNumber + " has not enough money.");
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("orderId", paymentId);
                jsonResponse.put("balance", balanceUser);
                jsonResponse.put("success", 0);
                jsonResponse.put("message", "Not enough money");
                this.publish(PAYMENT_ORDER_RESPONSES, this.setMessage(jsonResponse.toString()));
            } else {
                publishPaymentStatus(message, DatabaseManager.PENDING);
                authTransactionRequest(accountNumber, paymentId, this.setMessage(message));
            }
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }
    }

    /**
     * Process transaction and update all account balances
     * Here the whole money related stuff happens
     * @param message Mqtt Mesagge
     * @throws JSONException
     * @throws SQLException
     * @throws MqttException
     */
    private void makeTransaction(MqttMessage message) throws JSONException, SQLException, MqttException {

        // PREPARATION

        JSONObject json = new JSONObject(message.toString());
        String accountNumber = json.getString("sourceAccount");
        JSONArray destinations = json.getJSONArray("paymentDestinations");
        String destinationAccount;
        double totalPrice = json.getDouble("amount");

        logger.debug("Payment from: " + accountNumber + " is going to be processed");

        // PAYMENT PROCESS
        dm.updateAccountSum(accountNumber, (-1)*totalPrice);

        for(int i = 0; i < destinations.length(); i++) {
            JSONObject destination = destinations.getJSONObject(i);
            destinationAccount = destination.getString("destinationAccount");
            Account acc = dm.getUserAccountByNumber(destinationAccount);
            double price = destination.getDouble("amount");

            dm.updateAccountSum(acc.getAccountNumber(), price);
        }

        // SEND NOTIFICATIONS TO DESTINATION ACCOUNTS
        // TODO send notification to destination accounts, these represent merchants

        publishPaymentStatus(message.toString(), DatabaseManager.RECEIVED);
    }

    /**
     * Method on success will send POST with encoded JSON to {@link #linkAccountUrl} with newly added account to the database
     * Returns generated token in uuid format for given account number
     * Otherwise will print an error message
     * @param accountNumber stored in Bank DB
     * @return uuid String, generated token of account number
     */
    public String generateLinkAccString(final String accountNumber) throws UnsupportedEncodingException, SQLException, JSONException {

        final String uuid;

        // Shorter UUID for test purposes
        if(isDevel) {
            uuid = String.valueOf(UUID.randomUUID()).substring(0,6);
        }
        else {
            uuid = String.valueOf(UUID.randomUUID());
        }

        // TODO change IBAN check... now needs 24 chars
        if(accountNumber.length() != 24) {
            logger.error("Account number is not in valid IBAN format");
            return "";
        } else if (dm.getUserAccountByNumber(accountNumber).getId() != NULL) {
            logger.error("Account number is already in use");
            return "";
        }
        else {

            boolean status = dm.linkAccount(accountNumber, bic);

            if (status) {

                // REST POST
                final HttpPost post = new HttpPost(linkAccountUrl);

                String timestamp = new Timestamp(System.currentTimeMillis()).toString();

                // Request parameters and other properties
                JSONObject json = new JSONObject();
                json.put("accountNumber", accountNumber);
                json.put("token", uuid);
                json.put("timestamp", timestamp);
                StringEntity params = new StringEntity(json.toString());
                post.setHeader("content-type", "application/json");
                post.setEntity(params);

                // Start the client if not running
                if (!httpclient.isRunning()) {
                    httpclient.start();
                }

                httpclient.execute(post, new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse httpResponse) {
//                        logger.debug(post.getRequestLine() + "->" + httpResponse.getStatusLine());
                        logger.debug("Link to " + accountNumber + " was generated successfully with string: " + uuid);

                        InputStream inputStream = null;
                        try {
                            inputStream = httpResponse.getEntity().getContent();
                            String jsonString = convertStreamToString(inputStream);

                            logger.debug("HTTP request json received: " + jsonString);
                            JSONObject json = new JSONObject(jsonString);

                            boolean success = json.getBoolean("success");
                            String messageResponse = json.getString("message");

                            if (success) {
                                logger.debug("HTTP request successfully received. Adding transaction. Message: " + messageResponse);
                            } else {
                                logger.info("There was an error in receiving the message. Message: " + messageResponse);
                            }
                        } catch (IOException e) {
                            logger.error("Error: ", e);
                            e.printStackTrace();
                        } catch (JSONException e) {
                            logger.error("Error: ", e);
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failed(Exception e) {
                        logger.error(post.getRequestLine() + "->" + e);
                        e.printStackTrace();
                    }

                    @Override
                    public void cancelled() {
                        logger.error(post.getRequestLine() + " cancelled");
                    }
                });
            }
        }

        return uuid;
    }


    /**
     * Method will send Mqtt message with encapsulated data in JSON format about balance
     * @param accountNumber String, account number
     * @throws JSONException
     * @throws SQLException
     * @throws MqttException
     */
    public void getBalance(String accountNumber) throws JSONException, SQLException, MqttException {
        Account account = dm.getUserAccountByNumber(accountNumber);
        double balance = account.getBalance();
        Bank bank = dm.getBank(account.getBankId());

        String timestamp = new SimpleDateFormat(TIME_FORMAT).format(Calendar.getInstance().getTime());

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("bankId", bank.getBic());
        jsonResponse.put("accountNumber", accountNumber);
        jsonResponse.put("balance", balance);
        jsonResponse.put("currency", "EUR");
        jsonResponse.put("message", "balance");
        jsonResponse.put("time", timestamp);
        this.publish(RESPONSE_BALANCE + "/" + bank.getBic() + "/" + accountNumber, this.setMessage(jsonResponse.toString()));
    }


    /**
     * Method will send Mqtt message with encapsulated data in JSON format about history
     * with all informations related with each payment in history
     * @param accountNumber String, account number
     * @throws JSONException
     * @throws SQLException
     * @throws MqttException
     */
    public void getHistory(String accountNumber) throws JSONException, SQLException, MqttException {
        Account account = dm.getUserAccountByNumber(accountNumber);
        ArrayList<Transaction> transactions = dm.getHistory(account.getId());
        Bank bank = dm.getBank(account.getBankId());

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("bankId", bank.getBic());
        jsonResponse.put("accountNumber", account.getAccountNumber());
        jsonResponse.put("message", "history");
        JSONArray jsonArray = new JSONArray();
        for(Transaction t : transactions) {
            JSONObject jsonPayment = new JSONObject();
            jsonPayment.put("paymentId", t.getPaymentId());
            jsonPayment.put("status", t.getStatus());
            jsonPayment.put("amount", t.getAmount());
            jsonPayment.put("currency", "EUR");
            jsonPayment.put("time_sent", t.getDateCreated());
            jsonPayment.put("message", "message about payment");
            JSONArray destinations = new JSONArray();
            JSONObject destination = new JSONObject();
            ArrayList<AccountTransaction> accountTransactions = dm.getTransactionDestinations(t.getId());
            for(AccountTransaction at : accountTransactions) {
                destination.put("destinationAccount", at.getToId());
                destination.put("amount", at.getAmount());
                destinations.put(destination);
            }
            jsonPayment.put("paymentDestinations", destinations);
            jsonArray.put(jsonPayment);
        }
        jsonResponse.put("paymentOrders", jsonArray);
        this.publish(RESPONSE_HISTORY + "/" + bank.getBic() + "/" + accountNumber, this.setMessage(jsonResponse.toString()));
    }


    /**
     * Method will send POST request to {@link #transactionAuthUrl} for transaction authentication
     * @param accountNumber String, account number
     * @param paymentId String, id of payment in uuid format
     * @param message Mqtt message
     * @throws InterruptedException
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    private void authTransactionRequest(String accountNumber, final String paymentId, final MqttMessage message) throws InterruptedException, JSONException, UnsupportedEncodingException {

        double amount = getTotalAmount(message.toString());

        // Start the client if not started
        if(!httpclient.isRunning()) {
            httpclient.start();
        }

        final HttpPost request = new HttpPost(transactionAuthUrl);

        String timestamp = new Timestamp(System.currentTimeMillis()).toString();

        // Request parameters and other properties.
        JSONObject json = new JSONObject();
        json.put("accountNumber", accountNumber);
        json.put("paymentId", paymentId);
        json.put("amount", String.valueOf(amount));
        json.put("timestamp", timestamp);
        StringEntity params = new StringEntity(json.toString());
        request.setHeader("content-type", "application/json");
        request.setEntity(params);

        // Async task HTTP request
        httpclient.execute(request, new FutureCallback<HttpResponse>() {

            public void completed(final HttpResponse response) {
//                logger.debug(request.getRequestLine() + "->" + response.getStatusLine());
                try {
                    InputStream inputStream = response.getEntity().getContent();
                    String jsonString = convertStreamToString(inputStream);

                    logger.debug("HTTP request json received: " + jsonString);
                    JSONObject json = new JSONObject(jsonString);

                    boolean success = json.getBoolean("success");
                    String messageResponse = json.getString("message");

                    if(success) {
                        logger.debug("HTTP request transaction authentication - Successfully received.");
                    }
                    else {
                        updatePaymentStatus(paymentId, DatabaseManager.REJECTED);
                        logger.error("Error in transaction confirmation - receiving POST, payment rejected");
                    }
                } catch (IOException | JSONException | SQLException | MqttException e) {
                    logger.error("Error: ", e);
                    e.printStackTrace();
                }
            }

            public void failed(final Exception ex) {
                try {
                    updatePaymentStatus(paymentId, DatabaseManager.REJECTED);
                } catch (SQLException | MqttException | JSONException e) {
                    logger.error("Error: ", e);
                    e.printStackTrace();
                }
                logger.error(request.getRequestLine() + "->" + ex);
            }

            public void cancelled() {
                try {
                    updatePaymentStatus(paymentId, DatabaseManager.REJECTED);
                } catch (SQLException | MqttException | JSONException e) {
                    logger.error("Error: ", e);
                    e.printStackTrace();
                }
                logger.error(request.getRequestLine() + " cancelled");
            }

        });
    }


    /**
     * Method will send POST request to {@link #authUrl} for request authentication
     * @param accountNumber String, account number
     * @param action String, action of request find in {@see BankRest/IdentityMessage class}
     * @param message Mqtt emssage
     * @throws InterruptedException
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    public void authRequest(String accountNumber, final String action, final MqttMessage message) throws InterruptedException, JSONException, UnsupportedEncodingException {

        // Start the client if not started
        if(!httpclient.isRunning()) {
            httpclient.start();
        }

        final HttpPost request = new HttpPost(authUrl);
        final String uuid = String.valueOf(UUID.randomUUID());

        String timestamp = new Timestamp(System.currentTimeMillis()).toString();

        JSONObject json = new JSONObject();
        json.put("accountNumber", accountNumber);
        json.put("timestamp", timestamp);
        json.put("guid", uuid);
        json.put("action", action);
        StringEntity params = new StringEntity(json.toString());
        request.setHeader("content-type", "application/json");
        request.setEntity(params);

        // Async task with HTTP request
        httpclient.execute(request, new FutureCallback<HttpResponse>() {

            public void completed(final HttpResponse response) {
//                logger.debug(request.getRequestLine() + "->" + response.getStatusLine());
                try {
                    InputStream inputStream = response.getEntity().getContent();
                    String jsonString = convertStreamToString(inputStream);

                    logger.debug("HTTP request json received: " + jsonString);
                    JSONObject json = new JSONObject(jsonString);

                    boolean success = json.getBoolean("success");
                    String messageResponse = json.getString("message");

                    if(success) {
                        logger.debug("HTTP request identity confirmation - Successfully received.");
                    }
                    else {
                        // if transaction was requested reject it if error in receiving POST message
                        if(action.equals(IdentityMessage.TRANSACTION_ACTION)) {
                            sendRejectedTransactionMessage(message);
                        }
                        logger.error("Identity confirmation error - receiving POST");
                    }
                } catch (IOException | JSONException e) {
                    logger.error("Error: ", e);
                    e.printStackTrace();
                }
            }

            public void failed(final Exception ex) {
                // if transaction was requested reject it if error in receiving POST message
                if(action.equals(IdentityMessage.TRANSACTION_ACTION)) {
                    sendRejectedTransactionMessage(message);
                }
                logger.error(request.getRequestLine() + "->" + ex);
            }

            public void cancelled() {
                // if transaction was requested reject it if error in receiving POST message
                if(action.equals(IdentityMessage.TRANSACTION_ACTION)) {
                    sendRejectedTransactionMessage(message);
                }
                logger.error(request.getRequestLine() + " cancelled");
            }
        });
    }


    /**
     * Method used for processing rejectd transaction messages
     * @param message Mqtt message
     */
    private void sendRejectedTransactionMessage(MqttMessage message){
        JSONObject jsonPayment = null;
        try {
            jsonPayment = new JSONObject(message.toString());
            String paymentId = jsonPayment.getString("paymentId");
            updatePaymentStatus(paymentId, DatabaseManager.REJECTED);
        } catch (JSONException | SQLException | MqttException e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }
    }

    /**
     * Converting InputStream of characters into String
     * @param is InputStream
     * @return String converted from InputStream
     */
    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Check if String is encoded as JSON
     * @param message String message
     * @return boolean
     */
    private boolean isJSONObject(String message) {
        String firstChar = String.valueOf(message.charAt(0));

        if(firstChar.equalsIgnoreCase("[")) {
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Method returns the total amount of money in payment for all payment destinations
     * @param message String, converted from Mqtt Message, encoded Json in string
     * @return double, total amount
     * @throws JSONException on exception
     */
    private double getTotalAmount(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);
        JSONArray destinations = jsonObject.getJSONArray("paymentDestinations");
        double totalPrice = 0;

        for(int i = 0; i < destinations.length(); i++) {
            JSONObject destination = destinations.getJSONObject(i);
            totalPrice += destination.getDouble("amount");
        }

        return totalPrice;
    }

    /**
     * Convert Transaction into json encoded message in String with all payment destinations
     * @param tr Transaction object
     * @return String, encoded Transaction into json format in String
     * @throws JSONException
     * @throws SQLException
     */
    public String createMessageFromTransaction(Transaction tr) throws JSONException, SQLException {

        JSONObject jsonResponse = new JSONObject();
        JSONArray destinations = new JSONArray();

        String timestamp = new SimpleDateFormat(TIME_FORMAT).format(Calendar.getInstance().getTime());

        Account acc = dm.getUserAccount(tr.getFromId());
        String accountNumber = acc.getAccountNumber();

        jsonResponse.put("paymentId", tr.getPaymentId());
        jsonResponse.put("status", tr.getStatus());
        jsonResponse.put("amount", tr.getAmount());
        jsonResponse.put("bankId", acc.getBankId());
        jsonResponse.put("sourceAccount", accountNumber);
        jsonResponse.put("currency", "EUR");
        jsonResponse.put("timeSent", timestamp);
        jsonResponse.put("message", tr.getMessage());
        ArrayList<AccountTransaction> paymentDestinations = dm.getTransactionDestinations(tr.getId());
        for(AccountTransaction at : paymentDestinations) {
            JSONObject destination = new JSONObject();
            Account account = dm.getUserAccount(at.getToId());
            destination.put("destinationAccount", account.getAccountNumber());
            destination.put("amount", at.getAmount());
            destinations.put(destination);
        }

        jsonResponse.put("paymentDestinations", destinations);

        return jsonResponse.toString();
    }


    /**
     * Method used for processing transactions by payment id.
     * Transaction is obtained from database (if successfully) and send to be updated, otherwise nothing proceeds
     * @param paymentId String, payment id in UUID format
     * @param status String status of message {@see BankModule/Manager/DatabaseManager class}
     * @throws SQLException
     * @throws MqttException
     * @throws JSONException
     */
    public void updatePaymentStatus(String paymentId, String status) throws SQLException, MqttException, JSONException {
        Transaction tr = dm.getTransactionByPaymentId(paymentId);

        if(tr.getId() != NULL) {
            JSONObject jsonResponse = new JSONObject();
            JSONArray destinations = new JSONArray();

            String timestamp = new SimpleDateFormat(TIME_FORMAT).format(Calendar.getInstance().getTime());

            Account acc = dm.getUserAccount(tr.getFromId());
            String accountNumber = acc.getAccountNumber();

            jsonResponse.put("paymentId", paymentId);
            jsonResponse.put("status", status);
            jsonResponse.put("amount", tr.getAmount());
            jsonResponse.put("bankId", acc.getBankId());
            jsonResponse.put("sourceAccount", accountNumber);
            jsonResponse.put("currency", "EUR");
            jsonResponse.put("timeSent", timestamp);
            jsonResponse.put("message", tr.getMessage());
            ArrayList<AccountTransaction> paymentDestinations = dm.getTransactionDestinations(tr.getId());
            for(AccountTransaction at : paymentDestinations) {
                JSONObject destination = new JSONObject();
                Account account = dm.getUserAccount(at.getToId());
                destination.put("destinationAccount", account.getAccountNumber());
                destination.put("amount", at.getAmount());
                destinations.put(destination);
            }

            jsonResponse.put("paymentDestinations", destinations);

            if(status.equals(DatabaseManager.RECEIVED)) {
                makeTransaction(setMessage(jsonResponse.toString()));
            }
            else {
                publishPaymentStatus(jsonResponse.toString(), status);
            }
        }
    }


    /**
     * Method used for publishing transaction status information
     * Transaction is obtained from database and (if successfully) updated, otherwise new transaction is created
     * @param message String, encoded transaction in json in String
     * @param status String status of message {@see BankModule/Manager/DatabaseManager class}
     * @return boolean, true -> payment status was published with Mqtt, false -> payment status was not published
     */
    public boolean publishPaymentStatus(String message, String status) {
        JSONObject json = null;
        JSONObject jsonResponse = new JSONObject();
        JSONArray destinations = new JSONArray();
        Account account = null;
        String paymentId = "";
        String msg = "";
        double amount = 0;

        try {

            json = new JSONObject(message);
            paymentId = json.getString("paymentId");
            amount = getTotalAmount(message);
            Transaction tr;
            String timestamp = new SimpleDateFormat(TIME_FORMAT).format(Calendar.getInstance().getTime());
            String accountNumber = json.getString("sourceAccount");
            destinations = json.getJSONArray("paymentDestinations");

            jsonResponse.put("paymentId", paymentId);
            jsonResponse.put("status", status);
            jsonResponse.put("amount", amount);
            jsonResponse.put("bankId", json.get("bankId"));
            jsonResponse.put("sourceAccount", accountNumber);
            jsonResponse.put("currency", json.get("currency"));
            jsonResponse.put("timeSent", timestamp);
            jsonResponse.put("message", json.get("message"));
            jsonResponse.put("paymentDestinations", json.getJSONArray("paymentDestinations"));

            // if transaction exists update status, else create new transaction
            if((tr = dm.getTransactionByPaymentId(paymentId)).getId() != NULL) {
                // if transaction is going to be changed from requested to pending or from pending to any state except requested
                if((tr.getStatus().equals(DatabaseManager.REQUESTED) && status.equals(DatabaseManager.PENDING)) ||
                        (tr.getStatus().equals(DatabaseManager.PENDING) && !status.equals(DatabaseManager.REQUESTED))) {
                    dm.updateTransactionStatus(tr.getId(), status, timestamp);
                    publish(PAYMENT_ORDER_RESPONSES, setMessage(jsonResponse.toString()));
                }
                else {
                    logger.error("Transaction with payment id: " + tr.getPaymentId() + " was already processed.");
                    return false;
                }
            }
            else {

                msg = json.getString("message");
                account = dm.getUserAccountByNumber(accountNumber);

                int transactionId = dm.insertTransaction(account.getId(), paymentId, amount, status, msg);
                for (int i = 0; i < destinations.length(); i++) {
                    JSONObject destination = destinations.getJSONObject(i);
                    String destinationAccount = destination.getString("destinationAccount");
                    dm.linkTransactionDestination(destinationAccount, transactionId, destination.getDouble("amount"), msg);
                }

                // Requested is only proceed before confirmation
                if(!status.equals(DatabaseManager.REQUESTED)) {
                    publish(PAYMENT_ORDER_RESPONSES, setMessage(jsonResponse.toString()));
                }
            }

        } catch (MqttException | SQLException | JSONException e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }

        return true;
    }

}
