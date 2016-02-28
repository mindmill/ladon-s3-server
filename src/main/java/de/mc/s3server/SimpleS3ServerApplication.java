/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;


/**
 * Main Application class
 *
 * @author Ralf Ulrich
 */
@SpringBootApplication(exclude = {DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class})
public class SimpleS3ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleS3ServerApplication.class, args);
    }
}
