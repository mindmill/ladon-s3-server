package de.mc.ladon.s3server.jetty;

import de.mc.ladon.s3server.logging.LoggingRepository;
import de.mc.ladon.s3server.repository.impl.FSRepository;
import de.mc.ladon.s3server.servlet.S3Servlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;

/**
 * ladon S3 server example using Jetty
 * @author Ralf Ulrich 27/07/2021
 */
public class S3JettyServer {

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        String s3Data = System.getProperty("user.home") + File.separator + ".s3server";
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        S3Servlet s3Servlet = new S3Servlet(10);
        s3Servlet.setRepository(new LoggingRepository(new FSRepository(s3Data, "4aWji2M7heiCuPsJu9UQ78UE")));
        ServletHolder servletHolder = new ServletHolder("s3servlet", s3Servlet);
        handler.addServletWithMapping(servletHolder, "/*");
        server.start();
        server.join();
    }
}
