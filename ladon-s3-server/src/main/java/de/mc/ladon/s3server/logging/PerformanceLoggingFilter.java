/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

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
        String path = ((HttpServletRequest) request).getPathInfo();
        String method = ((HttpServletRequest) request).getMethod();


        startTime = System.currentTimeMillis();
        chain.doFilter(request, response);
        endTime = System.currentTimeMillis();

        //Log the path and time taken
        perfLogger.info(method + " - " + path + ", time: " + (endTime - startTime) + " ms");
    }


    public void destroy() {

    }

}
