/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.config;

import de.mc.ladon.s3server.enc.AESFileEncryptor;
import de.mc.ladon.s3server.logging.LoggingRepository;
import de.mc.ladon.s3server.logging.PerformanceLoggingFilter;
import de.mc.ladon.s3server.repository.api.S3Repository;
import de.mc.ladon.s3server.repository.impl.FSRepository;
import de.mc.ladon.s3server.servlet.S3Servlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * Configuration of beans for the s3server
 *
 * @author Ralf Ulrich on 17.02.16.
 */
@Configuration
public class BeanConfig {

    @Value("${s3server.fsrepo.root}")
    String fsRepoRoot;
    @Value("${s3server.fsrepo.secret")
    String encSecret;

    @ConditionalOnMissingBean
    @Bean
    S3Repository s3Repository() {
        return new FSRepository(fsRepoRoot, new AESFileEncryptor(encSecret.getBytes(StandardCharsets.UTF_8)));
    }

    @Bean(destroyMethod = "destroy")
    S3Servlet s3Servlet(S3ServletConfiguration config, S3Repository repository) {
        S3Servlet servlet = new S3Servlet(config.getThreadPoolSize());
        if (config.isLoggingEnabled()) {
            servlet.setRepository(new LoggingRepository(repository));
        } else {
            servlet.setRepository(repository);
        }
        servlet.setSecurityEnabled(config.isSecurityEnabled());
        return servlet;
    }

    @Bean
    ServletRegistrationBean<S3Servlet> s3Registration(S3ServletConfiguration config, S3Servlet s3Servlet) {
        ServletRegistrationBean<S3Servlet> bean = new ServletRegistrationBean<>(s3Servlet);
        bean.setName("s3servlet");
        bean.setAsyncSupported(true);
        bean.addUrlMappings(config.getBaseUrl() + "/*");
        return bean;
    }

    @Bean
    @ConditionalOnProperty(value = "s3server.loggingEnabled", havingValue = "true")
    FilterRegistrationBean<PerformanceLoggingFilter> filterRegistrationBean() {
        FilterRegistrationBean<PerformanceLoggingFilter> filterBean = new FilterRegistrationBean<>();
        filterBean.setFilter(new PerformanceLoggingFilter());
        filterBean.addServletNames("s3servlet");
        filterBean.setAsyncSupported(true);
        return filterBean;
    }
}
