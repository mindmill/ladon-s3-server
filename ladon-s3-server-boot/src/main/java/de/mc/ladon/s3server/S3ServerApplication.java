/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Main Application class
 *
 * @author Ralf Ulrich
 */
@SpringBootApplication
public class S3ServerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(S3ServerApplication.class);
        app.setBanner((environment, aClass, printStream) -> {
            printStream.append("Mind S3 Server\n");
        });
        app.run(args);
    }
}
