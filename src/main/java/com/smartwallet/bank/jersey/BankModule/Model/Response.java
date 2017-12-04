/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.smartwallet.bank.jersey.BankModule.Model;

import java.io.InputStream;

/**
 * Class for HTTP Response
 * @author Martin Stepanek
 */
public class Response {
    private InputStream body;

    public Response(InputStream body) {
        this.body = body;
    }

    public InputStream getBody() {
        return body;
    }
}
