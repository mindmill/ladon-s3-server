/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.jaxb.entities;

import de.mc.s3server.jaxb.S3DateAdapter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
@XmlRootElement(name = "Bucket")
public class Bucket {

    private String name;
    private Date creationDate;

    public Bucket() {
    }

    public Bucket(String name, Date creationDate) {
        this.name = name;
        this.creationDate = creationDate;
    }


    @XmlElement(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "CreationDate")
    @XmlJavaTypeAdapter(S3DateAdapter.class)
    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
