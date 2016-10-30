/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.entities;

import de.mc.ladon.s3server.entities.api.S3CallContext;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
@XmlRootElement(name = "ListVersionsResult")
public class ListVersionsResult {

    private String name;
    private String prefix;
    private String keyMarker;
    private String nextKeyMarker;
    private String nextVersionIdMarker;
    private Integer maxKeys;
    private Boolean isTruncated;
    private List<AbstractVersionElement> versions;

    public ListVersionsResult() {
    }

    public ListVersionsResult(S3CallContext callContext,
                              String bucketName,
                              List<AbstractVersionElement> versions,
                              boolean isTruncated,
                              String nextKeyMarker,
                              String nextVersionIdMarker) {
        this.name = bucketName;
        this.prefix = callContext.getParams().getPrefix();
        this.keyMarker = callContext.getParams().getKeyMarker();
        this.maxKeys = callContext.getParams().getMaxKeys();
        this.isTruncated = isTruncated;
        this.versions = versions;
        this.nextKeyMarker = nextKeyMarker;
        this.nextVersionIdMarker = nextVersionIdMarker;
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

    @XmlElement(name = "KeyMarker")
    public String getKeyMarker() {
        return keyMarker;
    }

    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }

    @XmlElement(name = "NextKeyMarker")
    public String getNextKeyMarker() {
        return nextKeyMarker;
    }


    public void setNextKeyMarker(String nextKeyMarker) {
        this.nextKeyMarker = nextKeyMarker;
    }

    @XmlElement(name = "NextVersionIdMarker")
    public String getNextVersionIdMarker() {
        return nextVersionIdMarker;
    }

    public void setNextVersionIdMarker(String nextVersionIdMarker) {
        this.nextVersionIdMarker = nextVersionIdMarker;
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

    @XmlElements({@XmlElement(name = "Version", type = Version.class), @XmlElement(name = "DeleteMarker", type = DeleteMarker.class)})
    public List<AbstractVersionElement> getVersions() {
        return versions;
    }

    public void setVersions(List<AbstractVersionElement> versions) {
        this.versions = versions;
    }
}
