/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.mobilewallet.bank.jersey.BankRest;

import com.mobilewallet.bank.jersey.BankModule.BankApp;
import com.mobilewallet.bank.jersey.BankModule.BankClient;
import com.mobilewallet.bank.jersey.BankModule.*;
import com.mobilewallet.bank.jersey.BankModule.Manager.DatabaseManager;
import com.mobilewallet.bank.jersey.BankModule.Model.Transaction;
import com.mobilewallet.bank.jersey.BankModule.Test.Client;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.sql.Types.NULL;

/**
 * Class defines all rest services for the bank server
 * @author Martin Stepanek
 */

@Path("/rest")
public class BankRest {

    private final Logger logger = LoggerFactory.getLogger(BankRest.class);

    /**
     * Connect all bank clients, if were disconnected
     * On server startup all clients are connected automatically
     * @return status message
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/connect")
    public String getStatusConn() {
        BankApp bank = BankApp.getInstance();
        return bank.startClients();
    }


    /**
     * Disconnect all clients
     * @return status message
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/disconnect")
    public String getStatusDisConn() {
        BankApp bank = BankApp.getInstance();
        return bank.endClients();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/status")
    public String getStatus() {
        return "status";
    }


    /**
     * Link account in path params
     * Method will generate special id for later adding account to database
     * @param bic bank identification number
     * @param accountNumber account number
     * @return status message
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/link-account/{bic}/{accountNumber}")
    public String linkAccount(@PathParam("bic") String bic, @PathParam("accountNumber") String accountNumber) {
        String message = "Bank not found.";
        BankApp bankApp = BankApp.getInstance();
        for(BankClient bankClient : bankApp.getBankClients()) {
            if(bankClient.getBic().equals(bic)) {
                try {
                    String uuid = bankClient.generateLinkAccString(accountNumber);
                    if(uuid != "") {
                        message = "Bank account POST sent. UUID is: " + uuid;
                        logger.info(message);
                    } else {
                        message = "There was an error with account number " + accountNumber;
                        logger.error(message);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return message;
    }

    /**
     * Rest for testing purpose only
     * Link account with query params
     * Method will generate special id for later adding account to database
     * @param am Account message that is decoded from json
     * @return status message
     */

    @POST
    @Path("/link-account-post")
    public String linkAccountPost(AccountMessage am) {

        String message = "";
        BankApp bankApp = BankApp.getInstance();
        String bic = "";
        String accountNumber = am.getAccountNumber();
        JSONObject json = new JSONObject();

        if(accountNumber.length() >=8) {
            bic = accountNumber.substring(4, 8);
        }
        else {
            message = "Wrong length of account number: " + accountNumber;
            logger.error(message);
            try {
                json.put("success", false);
                json.put("message", message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json.toString();
        }

        for(BankClient bankClient : bankApp.getBankClients()) {
            if(bankClient.getBic().equals(bic)) {
                try {
                    String uuid = bankClient.generateLinkAccString(accountNumber);
                    if(uuid != "") {
                        message = "Bank account POST sent. UUID is: " + uuid;
                        logger.info(message);
                        json.put("message", "UUID is: " + uuid);
                        json.put("success", true);
                    } else {
                        message = "There was an error with account number " + accountNumber;
                        logger.error(message);
                        json.put("success", false);
                        json.put("message", message);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return json.toString();
    }


    /**
     * Transaction rest for updating transaction
     * @param tm Transaction message that is decoded from JSON
     * @return Response status
     */
    @POST
    @Path("/transaction")
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeTransactionStatus(TransactionMessage tm) {
        Response resp = Response.ok().build();

        logger.debug("Transaction confirmation for: " + tm.getPaymentId() + ", " + tm.getStatus());

        BankApp bankApp = BankApp.getInstance();
        DatabaseManager dm = DatabaseManager.getInstance();
        String bic = "";

        try {
            Transaction tr = dm.getTransactionByPaymentId(tm.getPaymentId());
            if(tr.getId() != NULL && (
                    tm.getStatus().equals(TransactionMessage.CONFIRMED) ||
                    tm.getStatus().equals(TransactionMessage.ERROR) ||
                    tm.getStatus().equals(TransactionMessage.EXPIRED) ||
                    tm.getStatus().equals(TransactionMessage.REJECTED))) {
                bic = dm.getBank(dm.getUserAccount(tr.getFromId()).getBankId()).getBic();
            }
            else {
                logger.error("Wrong json format or data format.");
                return Response.status(501).build();
            }
            // Check if transaction is already in DB
            if(!tr.getStatus().equals(DatabaseManager.PENDING)) {
                logger.error("Transaction can not be processed twice.");
                return Response.status(501).build();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for(BankClient bankClient : bankApp.getBankClients()) {
            if(bankClient.getBic().equals(bic)) {
                try {
                    // Publish update
                    bankClient.updatePaymentStatus(tm.getPaymentId(), tm.getStatusForDB());
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
        return resp;
    }

    /**
     * Identity confirmation rest for updating transaction
     * @param im Identity message that is decoded from JSON
     * @return Response status
     */
    @POST
    @Path("/identity")
    @Produces(MediaType.APPLICATION_JSON)
    public Response doRequestedAction(IdentityMessage im) {
        Response resp = Response.ok().build();

        logger.debug("Identity confirmation for: " + im.getAccountNumber() + ", " + im.getAction());

        BankApp bankApp = BankApp.getInstance();
        DatabaseManager dm = DatabaseManager.getInstance();
        String accountNumber = im.getAccountNumber();
        String action = im.getAction();
        String bic = "";
        try {
            bic = dm.getBank(dm.getUserAccountByNumber(accountNumber).getBankId()).getBic();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for(BankClient bankClient : bankApp.getBankClients()) {
            if(bankClient.getBic().equals(bic)) {
                try {
                    // Publish update
                    if(action.equals(IdentityMessage.BALANCE_ACTION)) {
                        bankClient.getBalance(accountNumber);
                    }
                    if(action.equals(IdentityMessage.HISTORY_ACTION)) {
                        bankClient.getHistory(accountNumber);
                    }
                    if(action.equals(IdentityMessage.TRANSACTION_ACTION)) {
                        ArrayList<Transaction> transactions = new ArrayList<>();
                        transactions = dm.getTransactionsByAccNumberStatus(accountNumber, DatabaseManager.REQUESTED);
                        for(Transaction t : transactions) {
                            bankClient.paymentOrderRequestByPaymentId(t.getPaymentId());
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return resp;
    }


    /**
     * For test purposes request history or balance with connected test client {@see BankServer class}
     * @return status message
     */
    @GET
    @Path("/test-mqtt-balance-history/{type}")
    public Response testMqttHistory(@PathParam("type") String type) {
        Response resp = Response.ok().build();
        String message = "Test client not connected";

        if(type.equals("history") || type.equals("balance")) {
            BankApp bankApp = BankApp.getInstance();
            Client testClient = bankApp.getTestClient();

            if (testClient != null) {
                // Create Mqtt request for history
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("accountNumber", testClient.getAccountNumber());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                message = jsonObject.toString();

                try {
                    testClient.publish(BankClient.BANK_TOPIC + "/request/" + type + "/" + testClient.getBic() + "/" + testClient.getAccountNumber(), testClient.setMessage(message));
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                logger.debug("JSON sent in Mqtt message: " + message);
            } else {
                logger.error(message);
                return Response.status(501).build();
            }
        }
        else {
            message = "Define history or balance in path param";
            logger.error(message);
            return Response.status(501).build();
        }

        return resp;
    }


    /**
     * For test purposes request history or balance with connected test client {@see BankServer class}
     * @return status message
     */
    @POST
    @Path("/test-mqtt-transaction")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testMqttTransaction(TestTransactionMessage ttm) {
        Response resp = Response.ok().build();
        String message = "Test client not connected";

        BankApp bankApp = BankApp.getInstance();
        Client testClient = bankApp.getTestClient();

        if (testClient != null) {
            // Create Mqtt request for transaction
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            ArrayList<Destination> destinations = ttm.getDestinations();
            try {
                jsonObject.put("paymentId", ttm.getPaymentId());
                jsonObject.put("bankId", testClient.getBic());
                jsonObject.put("sourceAccount", testClient.getAccountNumber());
                jsonObject.put("currency", ttm.getCurrency());
                jsonObject.put("time_sent", new SimpleDateFormat(BankClient.TIME_FORMAT).format(Calendar.getInstance().getTime()));
                jsonObject.put("message", ttm.getMessage());
                JSONObject destination;
                for(Destination d : destinations) {
                    destination =  new JSONObject();
                    destination.put("destinationAccount", d.getDestinationAccount());
                    destination.put("amount", d.getAmount());
                    jsonArray.put(destination);
                }
                jsonObject.put("paymentDestinations", jsonArray);
                message = jsonObject.toString();

                testClient.publish(BankClient.PAYMENT_ORDERS + "/" + testClient.getBic(), testClient.setMessage(message));
            } catch (MqttException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            logger.debug("JSON sent in Mqtt message: " + message);
        } else {
            logger.error(message);
            return Response.status(501).build();
        }

        return resp;
    }
}
