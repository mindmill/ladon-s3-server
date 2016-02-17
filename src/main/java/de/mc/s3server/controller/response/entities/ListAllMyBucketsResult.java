/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.controller.response.entities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by max on 17.02.16.
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
