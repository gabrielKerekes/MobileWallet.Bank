/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.smartwallet.bank.jersey.BankModule.Model;

/**
 * Shop java class from database
 * @author Martin Stepanek
 */
public class Shop {
    private int id;
    private String shopId;
    private double price;

    public Shop(int id, String shopId, double price) {
        this.id = id;
        this.shopId = shopId;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
