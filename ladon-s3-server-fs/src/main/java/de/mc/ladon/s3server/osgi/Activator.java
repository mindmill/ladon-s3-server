/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.osgi;

import de.mc.ladon.s3server.enc.AESFileEncryptor;
import de.mc.ladon.s3server.repository.api.S3Repository;
import de.mc.ladon.s3server.repository.impl.FSRepository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @author Ralf Ulrich on 11.03.16.
 */
public class Activator implements BundleActivator {


    public void start(BundleContext context) throws Exception {

        Dictionary<String, String> props = new Hashtable<>();
        String home = System.getProperty("user.home");
        context.registerService(S3Repository.class, new FSRepository(home + "/.s3server" ,
                new AESFileEncryptor("4aWji2M7heiCuPsJu9UQ78UE".getBytes())), props);

    }


    public void stop(BundleContext context) throws Exception {
        context.ungetService(context.getServiceReference(S3Repository.class));
    }
}
