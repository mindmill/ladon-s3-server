/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.impl;

import com.google.common.base.Splitter;
import de.mc.ladon.s3server.entities.api.S3ACLGrantee;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ralf Ulrich on 27.02.16.
 */
public class S3ACLGranteeImpl implements S3ACLGrantee {


    public static final String ADDRESS_PREFIX = "emailAddress";
    public static final String ID_PREFIX = "id";
    public static final String URI_PREFIX = "uri";
    public static final int EQUAL_GAP = 1;
    private List<String> id;
    private List<String> email;
    private List<String> uri;

    public S3ACLGranteeImpl(String headerString) {
        if (headerString != null) {
            List<String> elements = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(headerString);
            email = elements.stream()
                    .filter(e -> e.startsWith(ADDRESS_PREFIX))
                    .map(f -> f.substring(ADDRESS_PREFIX.length() + EQUAL_GAP)).collect(Collectors.toList());
            id = elements.stream()
                    .filter(e -> e.startsWith(ID_PREFIX))
                    .map(f -> f.substring(ID_PREFIX.length() + EQUAL_GAP)).collect(Collectors.toList());
            uri = elements.stream()
                    .filter(e -> e.startsWith(URI_PREFIX))
                    .map(f -> f.substring(URI_PREFIX.length() + EQUAL_GAP)).collect(Collectors.toList());
        }
    }

    @Override
    public List<String> getId() {
        return id;
    }

    @Override
    public List<String> getEmail() {
        return email;
    }

    @Override
    public List<String> getUri() {
        return uri;
    }
}
