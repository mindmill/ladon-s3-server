/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.osgi;

import de.mc.ladon.s3server.logging.LoggingRepository;
import de.mc.ladon.s3server.repository.api.S3Repository;
import de.mc.ladon.s3server.servlet.S3Servlet;
import jakarta.servlet.Servlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @author Ralf Ulrich on 11.03.16.
 */
public class S3ServerActivator implements BundleActivator {

    private static final String CONTEXT_STRING = "/api/s3";
    private ServiceRegistration<Servlet> servletRegistration;

    public void start(BundleContext context) {
        S3Servlet s3Servlet = new S3Servlet(4);

        ServiceReference<S3Repository> repositoryReference = context.getServiceReference(S3Repository.class);
        if (repositoryReference != null) {
            S3Repository repository = context.getService(repositoryReference);
            s3Servlet.setRepository(new LoggingRepository(repository));
        }

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("osgi.http.whiteboard.servlet.pattern", CONTEXT_STRING + "/*");
        props.put("osgi.http.whiteboard.servlet.name", "S3Servlet");

        servletRegistration = context.registerService(Servlet.class, s3Servlet, props);
    }

    public void stop(BundleContext context) {
        if (servletRegistration != null) {
            servletRegistration.unregister();
        }
    }
}
