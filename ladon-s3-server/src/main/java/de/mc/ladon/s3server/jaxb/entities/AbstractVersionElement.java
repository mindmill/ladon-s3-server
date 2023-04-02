/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.entities;

import de.mc.ladon.s3server.jaxb.S3DateAdapter;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
@XmlRootElement
@XmlSeeAlso({DeleteMarker.class,Version.class})
public abstract class AbstractVersionElement {
    private Owner owner;
    private String key;
    private Date lastModified;
    private String etag;
    private Long size;
    private String storageClass;
    private String versionId;
    private boolean isLatest;

    public AbstractVersionElement() {
    }

    public AbstractVersionElement(Owner owner,
                                  String key,
                                  String versionId,
                                  boolean isLatest,
                                  Date lastModified,
                                  String etag,
                                  Long size,
                                  String storageClass) {
        this.owner = owner;
        this.key = key;
        this.versionId = versionId;
        this.isLatest = isLatest;
        this.lastModified = lastModified;
        this.etag = etag;
        this.size = size;
        this.storageClass = storageClass;
    }

    @XmlElement(name = "Owner")
    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @XmlElement(name = "Key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @XmlElement(name = "VersionId")
    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    @XmlElement(name = "IsLatest")
    public boolean isLatest() {
        return isLatest;
    }

    public void setLatest(boolean latest) {
        isLatest = latest;
    }

    @XmlElement(name = "LastModified")
    @XmlJavaTypeAdapter(S3DateAdapter.class)
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @XmlElement(name = "ETag")
    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    @XmlElement(name = "Size")
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @XmlElement(name = "StorageClass")
    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }
}
