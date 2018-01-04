/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.mobilewallet.bank.jersey.BankModule.Manager;

import com.mobilewallet.bank.jersey.BankModule.Model.Account;
import com.mobilewallet.bank.jersey.BankModule.Model.AccountTransaction;
import com.mobilewallet.bank.jersey.BankModule.Model.Bank;
import com.mobilewallet.bank.jersey.BankModule.Model.*;
import com.mobilewallet.bank.jersey.BankModule.Model.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;

/**
 * Database manager handles all operation with data and database
 * @author Martin Stepanek
 */
public class DatabaseManager {

    /**
     * Table and columns definitions
     */
    public static final String ACCOUNT_TABLE = "account";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_BANK_ID = "bank_id";
    public static final String COLUMN_ACCOUNT_NUMBER = "account_number";
    public static final String COLUMN_BALANCE = "balance";
    public static final String COLUMN_DATE_CREATED = "date_created";

    public static final String BANK_TABLE = "bank";
    public static final String COLUMN_BIC = "bic";

    public static final String TRANSACTION_TABLE = "transaction";
    public static final String COLUMN_FROM_ID = "from_id";
    public static final String COLUMN_PAYMENT_ID = "payment_id";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_DATE_REALIZED = "date_realized";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_MESSAGE = "message";

    public static final String ACCOUNT_TRANSACTION_TABLE = "account_transaction";
    public static final String COLUMN_TO_ID = "to_id";
    public static final String COLUMN_TRANSACTION_ID = "transaction_id";


    /**
     * Database transaction message definitions
     */
    public static final String RECEIVED = "received";
    public static final String REJECTED = "rejected";
    public static final String EXPIRED = "expired";
    public static final String PENDING = "pending";
    public static final String REQUESTED = "requested";

    private final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    /**
     * Initial balance set when creating new account number, used for testing purposes
     */
    private static final double BALANCE_AMOUNT = 10000;

    private static DatabaseManager instance;

    private Connection conn;


    /**
     * Method for database connection
     * @return boolean, status
     */
    private boolean connect() {
        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            ConfigManager cm = ConfigManager.getInstance();
           conn = DriverManager.getConnection(cm.getPropValues("DB_URL"), cm.getPropValues("DB_USERNAME"), cm.getPropValues("DB_PASSWORD"));

            if (conn != null) {
                logger.info("Database successfully connected");
                return true;
            }
        } catch (SQLException ex) {
            logger.error("Error: ", ex);
            ex.printStackTrace();
        }
        return false;
    }


    /**
     * Database manager constructor
     */
    private DatabaseManager() {
        connect();
    }


    /**
     * Method for returning singleton instance
     * @return DatabaseManager
     */
    public static DatabaseManager getInstance() {
        if(instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    /**
     * Add new account to database with specific amount of mane on account {@link #BALANCE_AMOUNT}
     * @param accountNumber String, account number
     * @param bic String, bank identification code
     * @return boolean status
     * @throws SQLException Sql exception if account exist
     */
    public boolean linkAccount(String accountNumber, String bic) throws SQLException {
        boolean status = true;
        PreparedStatement statement;

        if(getUserAccountByNumber(accountNumber).getAccountNumber() != null) {
            logger.error("Account number already exists");
            status = false;
        }
        else {

            Bank bank = getBankByBic(bic);
            if(bank.getId() == 0) {
                logger.error("Bank doesn't exists");
                status = false;
            }

            String sql = "INSERT INTO " + ACCOUNT_TABLE + " (" + COLUMN_BANK_ID + ", " + COLUMN_ACCOUNT_NUMBER + ", " + COLUMN_BALANCE + ") VALUES (?, ?, ?)";

            statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, bank.getId());
            statement.setString(2, accountNumber);
            statement.setDouble(3, BALANCE_AMOUNT);

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                logger.info("Account: " + accountNumber + " has been successfully added");
            }
        }
        return status;
    }


    /**
     * Get all banks from database
     * @return ArrayList of Bank
     * @throws SQLException on SQL error
     */
    public ArrayList<Bank> getAllBanks() throws SQLException {
        String sql = "SELECT * FROM " + BANK_TABLE;

        ArrayList<Bank> banks = new ArrayList<>();

        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery(sql);

        while (result.next()){
            Bank bank = new Bank();
            bank.setId(result.getInt(1));
            bank.setBic(result.getString(2));
            bank.setShortName(result.getString(3));
            bank.setName(result.getString(4));

            String output = "Bank: %s - %s - %s";
            logger.debug(String.format(output, bank.getId(), bank.getBic(), bank.getShortName()));
            banks.add(bank);
        }

        return banks;
    }


    /**
     * Get user account by accountNumber
     * @param accountNumber String IBAN
     * @return Account object
     * @throws SQLException on SQL error
     */
    public Account getUserAccountByNumber(String accountNumber) throws SQLException {
        String sql = "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + COLUMN_ACCOUNT_NUMBER + "=?";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, accountNumber);
        ResultSet result = statement.executeQuery();

        Account acc = new Account();

        while (result.next()){
            acc.setId(result.getInt(1));
            acc.setBankId(result.getInt(2));
            acc.setAccountNumber(result.getString(3));
            acc.setBalance(result.getDouble(4));

            String output = "User: %s - %s - %s";
//            logger.debug(String.format(output, acc.getId(),  acc.getAccountNumber(), acc.getBalance()));

        }

        return acc;
    }


    /**
     * Get user account by user id
     * @param userId int
     * @return Account, object
     * @throws SQLException on SQL error
     */
    public Account getUserAccount(int userId) throws SQLException {
        String sql = "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + COLUMN_ID + "=?";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, userId);
        ResultSet result = statement.executeQuery();


        Account acc = new Account();

        while (result.next()){
            acc.setId(result.getInt(1));
            acc.setBankId(result.getInt(2));
            acc.setAccountNumber(result.getString(3));
            acc.setBalance(result.getDouble(4));

            String output = "User: %s - %s - %s";
//            logger.debug(String.format(output, acc.getId(),  acc.getAccountNumber(), acc.getBalance()));
        }

        return acc;
    }


    /**
     * Update account balance with change, it could be positive or negative number
     * @param accountNumber String, account number
     * @param change double, an amount to update with
     * @throws SQLException on SQL error
     */
    public void updateAccountSum(String accountNumber, double change) throws SQLException {
        String sql = "UPDATE "+ ACCOUNT_TABLE + " SET " + COLUMN_BALANCE + "= " + COLUMN_BALANCE + " + ? WHERE " + COLUMN_ACCOUNT_NUMBER + "=?";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setDouble(1, change);
        statement.setString(2, accountNumber);

        int rowsUpdated = statement.executeUpdate();
        if (rowsUpdated > 0) {
            logger.info("An account: " + accountNumber + " balance updated with change: " + change);
        }
    }


    /**
     * Get bank by id
     * @param bankId int, bank id
     * @return object, Bank
     * @throws SQLException on SQL error
     */
    public Bank getBank(int bankId) throws SQLException {
        String sql = "SELECT * FROM " + BANK_TABLE + " WHERE " + COLUMN_ID + "=?";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, bankId);
        ResultSet result = statement.executeQuery();

        Bank bank = new Bank();

        while (result.next()){
            bank.setId(result.getInt(1));
            bank.setBic(result.getString(2));
            bank.setShortName(result.getString(3));
            bank.setName(result.getString(4));
        }

        return bank;
    }


    /**
     * Get bank by bic
     * @param bic String, bank identification code
     * @return object, Bank
     * @throws SQLException
     */
    public Bank getBankByBic(String bic) throws SQLException {
        String sql = "SELECT * FROM " + BANK_TABLE + " WHERE " + COLUMN_BIC + "=?";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, bic);
        ResultSet result = statement.executeQuery();

        Bank bank = new Bank();

        while (result.next()){
            bank.setId(result.getInt(1));
            bank.setBic(result.getString(2));
            bank.setShortName(result.getString(3));
            bank.setName(result.getString(4));
        }

        return bank;
    }


    /**
     * Link transaction destination account to current transaction
     * @param accountNumber String, account number
     * @param transactionId int, id of transaction
     * @param amount double, sum sent to destination
     * @param message message for destination account
     * @return status, 0=> successful
     * @throws SQLException on SQL error
     */
    public int linkTransactionDestination(String accountNumber, int transactionId, double amount, String message) throws SQLException {
        PreparedStatement statement;
        int toId = getUserAccountByNumber(accountNumber).getId();
        String sql = "INSERT INTO " + ACCOUNT_TRANSACTION_TABLE + " (" + COLUMN_TO_ID + ", " + COLUMN_TRANSACTION_ID + ", " + COLUMN_AMOUNT + ", " + COLUMN_MESSAGE + ") VALUES (?,?,?,?)";

        statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, toId);
        statement.setInt(2, transactionId);
        statement.setDouble(3, amount);
        statement.setString(4, message);
        int rowsInserted = statement.executeUpdate();
        if (rowsInserted > 0) {
            logger.debug("Transaction destination: " + accountNumber + " has been added");
        }

        ResultSet rs = statement.getGeneratedKeys();
        if( rs.next() ) {
            return rs.getInt(1);
        }

        return 0;
    }


    /**
     * Gety transaction destinations for given transaction id
     * @param transactionId int, id of transaction
     * @return ArrayList of AccountTransaction
     * @throws SQLException on SQL error
     */
    public ArrayList<AccountTransaction> getTransactionDestinations(int transactionId) throws SQLException {
        String sql = "SELECT * FROM " + ACCOUNT_TRANSACTION_TABLE + " WHERE " + COLUMN_TRANSACTION_ID + "=?";
        ArrayList<AccountTransaction> accountTransactions = new ArrayList<>();

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, transactionId);
        ResultSet result = statement.executeQuery();

        while (result.next()){
            AccountTransaction at = new AccountTransaction();
            at.setId(result.getInt(1));
            at.setToId(result.getInt(2));
            at.setTransactionId(result.getInt(3));
            at.setAmount(result.getDouble(4));
            at.setMessage(result.getString(5));
            accountTransactions.add(at);
        }

        return accountTransactions;
    }


    /**
     * Insert transaction into database
     * @param fromId id of Account from
     * @param paymentId Payment id, UUID String
     * @param amount double, amount
     * @param status String status
     * @param message String message of transaction
     * @return status 0 => successful
     * @throws SQLException on SQL error
     */
    public int insertTransaction(int fromId, String paymentId, double amount, String status, String message) throws SQLException {
        PreparedStatement statement;

        String sql = "INSERT INTO " + TRANSACTION_TABLE + " (" + COLUMN_FROM_ID + ", " + COLUMN_PAYMENT_ID + ", " + COLUMN_AMOUNT + ", " + COLUMN_STATUS + ", " + COLUMN_MESSAGE + ") VALUES (?, ?, ?, ?, ?)";

        statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, fromId);
        statement.setString(2, paymentId);
        statement.setDouble(3, amount);
        statement.setString(4, status);
        statement.setString(5, message);

        int rowsInserted = statement.executeUpdate();
        if (rowsInserted > 0) {
            logger.debug("Transaction: " + paymentId + " has been added");
        }


        ResultSet rs = statement.getGeneratedKeys();
        if( rs.next() ) {
            return rs.getInt(1);
        }

        return 0;
    }


    /**
     * Get transaction by payment id
     * @param paymentId payment id string
     * @return object, Transaction
     * @throws SQLException on SQL error
     */
    public Transaction getTransactionByPaymentId(String paymentId) throws SQLException {
        String sql = "SELECT * FROM " + TRANSACTION_TABLE + " WHERE " + COLUMN_PAYMENT_ID + "=?";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, paymentId);
        ResultSet result = statement.executeQuery();

        Transaction t = new Transaction();

        while (result.next()){
            t.setId(result.getInt(1));
            t.setFromId(result.getInt(2));
            t.setPaymentId(result.getString(3));
            t.setAmount(result.getDouble(4));
            t.setDateCreated(result.getDate(5));
            t.setDateRealized(result.getDate(6));
            t.setStatus(result.getString(7));
            t.setMessage(result.getString(8));
        }

        return t;
    }


    /**
     * Get all transactions from account number with given status
     * @param accountNumber String, account number
     * @param status String, status of transaction
     * @return ArrayList of Transaction
     * @throws SQLException on SQL error
     */
    public ArrayList<Transaction> getTransactionsByAccNumberStatus(String accountNumber, String status) throws SQLException {
        int fromId = getUserAccountByNumber(accountNumber).getId();
        String sql = "SELECT * FROM " + TRANSACTION_TABLE + " WHERE " + COLUMN_FROM_ID + "=?" + " AND " + COLUMN_STATUS + "=?" +
                " ORDER BY " + COLUMN_DATE_CREATED + " DESC";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, fromId);
        statement.setString(2, status);
        ResultSet result = statement.executeQuery();

        ArrayList<Transaction> transactions = new ArrayList<>();

        while (result.next()){
            Transaction t = new Transaction();
            t.setId(result.getInt(1));
            t.setFromId(result.getInt(2));
            t.setPaymentId(result.getString(3));
            t.setAmount(result.getDouble(4));
            t.setDateCreated(result.getDate(5));
            t.setDateRealized(result.getDate(6));
            t.setStatus(result.getString(7));
            t.setMessage(result.getString(8));

            transactions.add(t);
        }

        return transactions;
    }


    /**
     * Update transaction status
     * @param id int, id of transaction
     * @param status String, status of transaction
     * @param date String, Date in Mysql format for Date
     * @throws SQLException on SQL error
     */
    public void updateTransactionStatus(int id, String status, String date) throws SQLException {
        String sql = "UPDATE "+ TRANSACTION_TABLE + " SET " + COLUMN_STATUS + "= ? " + ", " + COLUMN_DATE_REALIZED + "= ? WHERE " + COLUMN_ID + "=?";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, status);
        statement.setString(2, date);
        statement.setInt(3, id);

        int rowsUpdated = statement.executeUpdate();
        if (rowsUpdated > 0) {
            logger.debug("A transaction status was updated successfully!");
        }
    }


    /**
     * Get history of transactions for given account
     * @param accountId int, id of account
     * @return ArrayList of Transaction
     * @throws SQLException on SQL error
     */
    public ArrayList<Transaction> getHistory(int accountId) throws SQLException {
        String sql = "SELECT * FROM " + TRANSACTION_TABLE + " WHERE " + COLUMN_FROM_ID + "=?";

        ArrayList<Transaction> transactions = new ArrayList();

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, accountId);
        ResultSet result = statement.executeQuery();

        while (result.next()){
            Transaction t = new Transaction();
            t.setId(result.getInt(1));
            t.setFromId(result.getInt(2));
            t.setPaymentId(result.getString(3));
            t.setAmount(result.getDouble(4));
            t.setDateCreated(result.getDate(5));
            t.setDateRealized(result.getDate(6));
            t.setStatus(result.getString(7));
            t.setMessage(result.getString(8));

            transactions.add(t);
        }

        return transactions;
    }


    /**
     * Get all accounts
     * @return ArrayList of Account
     * @throws SQLException on SQL error
     */
    public ArrayList<Account> getAllAccounts() throws SQLException {
        String sql = "SELECT * FROM " + ACCOUNT_TABLE;

        ArrayList<Account> accounts = new ArrayList<>();

        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery(sql);

        int count = 0;

        while (result.next()){
            Account acc = new Account();
            acc.setId(result.getInt(1));
            acc.setUserId(result.getString(2));
            acc.setBalance(result.getDouble(3));

            String output = "User #%d: %s - %s - %s";
            logger.debug(String.format(output, ++count, acc.getId(), acc.getUserId(), acc.getBalance()));
            accounts.add(acc);
        }

        return accounts;
    }

}
