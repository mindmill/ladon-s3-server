/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.impl;

import de.mc.ladon.s3server.entities.api.S3Metadata;
import de.mc.ladon.s3server.entities.api.S3ResponseHeader;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;

import static de.mc.ladon.s3server.common.S3Constants.*;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class S3ResponseHeaderImpl implements S3ResponseHeader {

    private Long contentLength;
    private String contentType;
    private Connection connection;
    private Date date;
    private Long expires;
    private Date lastModified;
    private String etag;
    private String server;
    private Boolean xamzDeleteMarker;
    private String xamzRequestId;
    private String xamzVersionId;
    private String xamzExpiration;
    private S3Metadata meta;

    public S3ResponseHeaderImpl() {
    }

    public S3ResponseHeaderImpl(S3Metadata meta) {
        this.meta = meta;
    }

    @Override
    public S3Metadata getMetadata() {
        return meta;
    }

    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public Long getContentLength() {
        return contentLength;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public void setExpires(Long expires) {
        this.expires = expires;
    }

    @Override
    public Long getExpires() {
        return expires;
    }

    @Override
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public void setEtag(String etag) {
        this.etag = etag;
    }

    @Override
    public String getEtag() {
        return etag;
    }

    @Override
    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public void setXamzDeleteMarker(Boolean xamzDeleteMarker) {
        this.xamzDeleteMarker = xamzDeleteMarker;
    }

    @Override
    public Boolean getXamzDeleteMarker() {
        return xamzDeleteMarker;
    }

    @Override
    public void setXamzRequestId(String xamzRequestId) {
        this.xamzRequestId = xamzRequestId;
    }

    @Override
    public String getXamzRequestId() {
        return xamzRequestId;
    }

    @Override
    public void setXamzVersionId(String xamzVersionId) {
        this.xamzVersionId = xamzVersionId;
    }

    @Override
    public String getXamzVersionId() {
        return xamzVersionId;
    }

    @Override
    public void setXamzExpiration(String expiration) {
        this.xamzExpiration = expiration;
    }

    @Override
    public String getXamzExpiration() {
        return xamzExpiration;
    }

    public static void appendHeaderToResponse(HttpServletResponse response, S3ResponseHeaderImpl header) {
        if (header.meta != null)
            header.meta.putInResponse(response);
        if (header.contentLength != null)
            response.setContentLengthLong(header.contentLength);
        if (header.contentType != null)
            response.setContentType(header.contentType);
        if (header.expires == null) {
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Cache-Control", "no-cache,no-store,max-age=0");
            response.setDateHeader("Expires", 1);
        } else {
            long now = System.currentTimeMillis();
            response.addHeader("Cache-Control", "max-age=" + header.expires);
            response.addHeader("Cache-Control", "must-revalidate");
            response.setDateHeader("Expires", now + (header.expires * 1000));
        }
        if (header.lastModified != null)
            response.setDateHeader("Last-Modified", header.lastModified.getTime());
        if (header.connection != null)
            response.setHeader(CONNECTION, header.connection.name());
        if (header.date != null)
            response.setDateHeader(DATE, header.date.getTime());
        if (header.etag != null)
            response.setHeader(ETAG, header.etag);
        if (header.server != null)
            response.setHeader(SERVER, header.server);
        if (header.xamzDeleteMarker != null)
            response.setHeader(X_AMZ_DELETE_MARKER, header.xamzDeleteMarker.toString());
        if (header.xamzRequestId != null)
            response.setHeader(X_AMZ_REQUEST_ID, header.xamzRequestId);
        if (header.xamzVersionId != null)
            response.setHeader(X_AMZ_VERSION_ID, header.xamzVersionId);
        if (header.xamzExpiration != null)
            response.setHeader(X_AMZ_EXPIRATION, header.xamzExpiration);
    }
}
