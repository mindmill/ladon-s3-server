/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.entities;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
@XmlRootElement(name = "ListAllMyBucketsResult")
public class ListAllMyBucketsResult {


    private Owner owner;
    private List<Bucket> bucketList;


    public ListAllMyBucketsResult() {
    }

    public ListAllMyBucketsResult(Owner owner) {
        this.owner = owner;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @XmlElement(name = "Bucket")
    @XmlElementWrapper(name="Buckets")
    public List<Bucket> getBucketList() {
        return bucketList;
    }

    public void setBucketList(List<Bucket> bucketList) {
        this.bucketList = bucketList;
    }
}
