/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.mobilewallet.bank.jersey.BankModule.Model;

import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Class for HTTP Request
 * @author Martin Stepanek
 */
public class Request implements Callable<Response> {
    private URL url;

    public Request(URL url) {
        this.url = url;
    }

    @Override
    public Response call() throws Exception {
        return new Response(url.openStream());
    }
}
