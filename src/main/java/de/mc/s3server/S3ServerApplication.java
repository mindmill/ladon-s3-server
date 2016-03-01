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
public class S3ServerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(S3ServerApplication.class);
        app.setBanner((environment, aClass, printStream) -> {
            printStream.append("Mind S3 Server\n");
        });
        app.run(args);
    }
}
