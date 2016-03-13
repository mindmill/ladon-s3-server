package de.mc.ladon.s3server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the S3Servlet
 *
 * @author Ralf Ulrich on 29.02.16.
 */
@Configuration
@ConfigurationProperties(prefix = "s3server")
public class S3ServletConfiguration {

    private String baseUrl = "/api/s3";
    private int threadPoolSize = 5;
    private boolean securityEnabled = true;


    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public void setSecurityEnabled(boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }
}
