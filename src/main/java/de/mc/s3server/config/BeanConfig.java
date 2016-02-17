/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Created by max on 17.02.16.
 */
@Configuration
public class BeanConfig {

    @Value("${s3.api.base.url}")
    private String apiPath;

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
