/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Performance logging filter to measure each request
 * @author by Ralf Ulrich on 18.03.16.
 */
public class PerformanceLoggingFilter implements Filter {


    private Logger perfLogger = LoggerFactory.getLogger(this.getClass());


    public void init(FilterConfig config) throws ServletException {

    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        long startTime;
        long endTime;
        HttpServletRequest servletRequest = ((HttpServletRequest) request);
        String path = servletRequest.getPathInfo();
        String method = servletRequest.getMethod();
        String query = servletRequest.getQueryString();
        Enumeration<String> headerNames = servletRequest.getHeaderNames();
        StringBuilder builder = new StringBuilder(" Header: ");
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String header =    servletRequest.getHeader(name);
                builder.append(name).append("=").append(header).append(", ");
            }
        }

        startTime = System.currentTimeMillis();
        chain.doFilter(request, response);
        endTime = System.currentTimeMillis();

        //Log the path and time taken
        perfLogger.info(method + " - " +  (endTime - startTime) + " ms - " + path + "?" + query + builder.toString());
    }


    public void destroy() {

    }

}
