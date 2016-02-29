/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.config;

import de.mc.s3server.repository.api.S3Repository;
import de.mc.s3server.repository.impl.FSRepository;
import de.mc.s3server.servlet.S3Servlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of beans for the s3server
 *
 * @author Ralf Ulrich on 17.02.16.
 */
@Configuration
public class BeanConfig {


    @ConditionalOnMissingBean
    @Bean
    S3Repository s3Repository() {
        return new FSRepository();
    }

    @Bean
    ServletRegistrationBean s3Registration(S3ServletConfiguration config, S3Repository repository) {
        ServletRegistrationBean bean = new ServletRegistrationBean();
        bean.setName("s3servlet");
        bean.setAsyncSupported(true);
        bean.addUrlMappings(config.getBaseUrl() + "/*");
        bean.setServlet(new S3Servlet(repository, config.getThreadPoolSize()));
        return bean;
    }


}
