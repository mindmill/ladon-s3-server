/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.entities;

import de.mc.ladon.s3server.entities.api.S3CallContext;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static de.mc.ladon.s3server.common.EncodingUtil.getEncoded;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
@XmlRootElement(name = "ListBucketResult")
public class ListBucketResult {

    private String bucketName;
    private String prefix;
    private String marker;
    private Integer maxKeys;
    private String delimiter;
    private Boolean isTruncated;
    private List<Contents> contentsList;
    private List<CommonPrefixes> commonPrefixes;

    public ListBucketResult() {
    }

    public ListBucketResult(S3CallContext callContext,
                            String bucketName,
                            List<Contents> contentsList,
                            List<CommonPrefixes> commonPrefixes,
                            boolean isTruncated) {
        this.bucketName = bucketName;
        this.prefix = getEncoded(callContext, callContext.getParams().getPrefix());
        this.marker = getEncoded(callContext, callContext.getParams().getMarker());
        this.maxKeys = callContext.getParams().getMaxKeys();
        this.delimiter = getEncoded(callContext, callContext.getParams().getDelimiter());
        this.isTruncated = isTruncated;
        this.contentsList = contentsList;
        this.commonPrefixes = commonPrefixes;
    }

    @XmlElement(name = "Name")
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
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

    @XmlElement(name = "Delimiter")
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
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


    @XmlElement(name = "CommonPrefixes")
    public List<CommonPrefixes> getCommonPrefixes() {
        return commonPrefixes;
    }

    public void setCommonPrefixesList(List<CommonPrefixes> commonPrefixes) {
        this.commonPrefixes = commonPrefixes;
    }
}
