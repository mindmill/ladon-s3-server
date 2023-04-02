///*
// * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
// */
//
//package de.mc.ladon.s3server.osgi;
//
//import de.mc.ladon.s3server.logging.LoggingRepository;
//import de.mc.ladon.s3server.repository.api.S3Repository;
//import de.mc.ladon.s3server.servlet.S3Servlet;
//import org.osgi.framework.BundleActivator;
//import org.osgi.framework.BundleContext;
//import org.osgi.framework.ServiceReference;
//import org.osgi.service.http.HttpService;
//import org.osgi.service.http.NamespaceException;
//import org.osgi.util.tracker.ServiceTracker;
//import org.osgi.util.tracker.ServiceTrackerCustomizer;
//
//import jakarta.servlet.ServletException;
//
///**
// * @author Ralf Ulrich on 11.03.16.
// */
//public class S3ServerActivator implements BundleActivator {
//    private ServiceTracker httpTracker;
//    private static final String CONTEXT_STRING = "/api/s3";
//    private S3Servlet s3Servlet;
//
//    public void start(BundleContext context) {
//
//
//        httpTracker = new ServiceTracker<>(context, HttpService.class,
//                new ServiceTrackerCustomizer<HttpService, Object>() {
//                    @Override
//                    public HttpService addingService(ServiceReference<HttpService> reference) {
//                        HttpService httpService = context.getService(reference);
//
//                        try {
//                            s3Servlet = new S3Servlet(4);
//                            ServiceReference<S3Repository> repositoryReference = context.getServiceReference(S3Repository.class);
//                            if (repositoryReference != null) {
//                                S3Repository repository = context.getService(repositoryReference);
//                                s3Servlet.setRepository(new LoggingRepository(repository));
//                            }
//                            httpService.registerServlet(CONTEXT_STRING, s3Servlet, null, null);
//                        } catch (ServletException | NamespaceException e) {
//                            //ignore
//                        }
//                        return null;
//                    }
//
//                    @Override
//                    public void modifiedService(ServiceReference<HttpService> reference, Object service) {
//                    }
//
//                    @Override
//                    public void removedService(ServiceReference<HttpService> reference, Object service) {
//                        HttpService httpService = context.getService(reference);
//                        httpService.unregister(CONTEXT_STRING);
//                    }
//
//                });
//
//        httpTracker.open();
//    }
//
//    public void stop(BundleContext context) {
//        httpTracker.close();
//    }
//
//
//}
