package de.mc.ladon.s3server.jetty;

import de.mc.ladon.s3server.enc.AESFileEncryptor;
import de.mc.ladon.s3server.logging.LoggingRepository;
import de.mc.ladon.s3server.repository.impl.FSRepository;
import de.mc.ladon.s3server.servlet.S3Servlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * ladon S3 server example using Jetty
 *
 * @author Ralf Ulrich 27/07/2021
 */
public class S3JettyServer {

    private static Logger logger = LoggerFactory.getLogger(S3JettyServer.class);

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        String defaultS3DataDir = System.getProperty("user.home") + File.separator + ".s3server";
        String s3Data = System.getProperty("s3server.fsrepo.root", defaultS3DataDir);
        logger.info("Storing data in " + s3Data);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        S3Servlet s3Servlet = new S3Servlet(10);
        s3Servlet.setRepository(new LoggingRepository(new FSRepository(s3Data,
                new AESFileEncryptor("4aWji2M7heiCuPsJu9UQ78UE".getBytes(StandardCharsets.UTF_8)))));
        ServletHolder servletHolder = new ServletHolder("s3servlet", s3Servlet);
        handler.addServletWithMapping(servletHolder, "/*");
        server.start();
        server.join();
    }
}
