/*
 * Copyright (c) 2017.
 * Bank application that is part of school team project.
 */

package com.smartwallet.bank.jersey.BankModule.Manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Config Manager, handle values in config file
 * @author Martin Stepanek
 */
public class ConfigManager {
    private String propFileName = "config.properties";
    private InputStream inputStream;
    private static ConfigManager instance;

    private final Logger logger = LoggerFactory.getLogger(ConfigManager.class);


    private ConfigManager() {}


    public static ConfigManager getInstance() {
        if(instance == null) {
            instance = new ConfigManager();
        }

        return instance;
    }


    /**
     * Method to get property value by name
     * @param name String, name of property
     * @return String, property value
     */
    public String getPropValues(String name) {
        Properties prop = new Properties();
        String result = "";
        inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

        try {
            if (inputStream != null) {
                    prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            result = prop.getProperty(name);

        } catch (IOException e) {
            logger.error("Error: ", e);
            e.printStackTrace();
        }

        return result;
    }
}
