/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.config;

import de.mc.s3server.repository.api.S3Repository;
import de.mc.s3server.repository.impl.FSRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
@Configuration
public class BeanConfig {

    @Value("${s3server.api.base.url}")
    private String apiPath;

    @Bean
    S3Repository s3Repository() {
        return new FSRepository();
    }

//    @Bean
//    FilterRegistrationBean rewriteFilter() {
//        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
//        registrationBean.addServletNames("dispatcherServlet");
//        registrationBean.setFilter(new UrlRewriteFilter());
//        registrationBean.addUrlPatterns( apiPath +"/*");
//        registrationBean.addInitParameter("confPath","urlrewrite.xml");
//        registrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
//        return registrationBean;
//    }


}
