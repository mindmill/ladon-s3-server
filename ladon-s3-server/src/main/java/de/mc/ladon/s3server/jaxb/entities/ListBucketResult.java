/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.entities;

import de.mc.ladon.s3server.entities.api.S3CallContext;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
@XmlRootElement(name = "ListBucketResult")
public class ListBucketResult {

    private String name;
    private String prefix;
    private String marker;
    private Integer maxKeys;
    private Boolean isTruncated;
    private List<Contents> contentsList;

    public ListBucketResult() {
    }

    public ListBucketResult(S3CallContext callContext, String bucketName, List<Contents> contentsList, boolean isTruncated) {
        this.name = bucketName;
        this.prefix = callContext.getParams().getPrefix();
        this.marker = callContext.getParams().getMarker();
        this.maxKeys = callContext.getParams().getMaxKeys();
        this.isTruncated = isTruncated;
        this.contentsList = contentsList;
    }

    @XmlElement(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "Prefix")
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @XmlElement(name = "Marker")
    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    @XmlElement(name = "MaxKeys")
    public Integer getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(Integer maxKeys) {
        this.maxKeys = maxKeys;
    }

    @XmlElement(name = "IsTruncated")
    public Boolean getIsTruncated() {
        return isTruncated;
    }

    public void setIsTruncated(Boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    @XmlElement(name = "Contents")
    public List<Contents> getContentsList() {
        return contentsList;
    }

    public void setContentsList(List<Contents> contentsList) {
        this.contentsList = contentsList;
    }
}
