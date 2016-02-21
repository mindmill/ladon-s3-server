/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Main Application class
 *
 * @author Ralf Ulrich
 */
@SpringBootApplication
public class SimpleS3ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleS3ServerApplication.class, args);
    }
}
