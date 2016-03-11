/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.entities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ralf Ulrich on 27.02.16.
 */
@XmlRootElement(name = "CreateBucketConfiguration")
public class CreateBucketConfiguration {

    private String locationConstraint;

    public CreateBucketConfiguration() {
    }

    public CreateBucketConfiguration(String locationConstraint) {
        this.locationConstraint = locationConstraint;
    }

    @XmlElement(name = "LocationConstraint")
    public String getLocationConstraint() {
        return locationConstraint;
    }

    public void setLocationConstraint(String locationConstraint) {
        this.locationConstraint = locationConstraint;
    }
}
