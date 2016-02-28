/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.entities.api.S3ResponseHeader;
import org.springframework.util.MimeType;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;

import static de.mc.s3server.common.S3Constants.*;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class S3ResponseHeaderImpl implements S3ResponseHeader {

    private Long contentLength;
    private MimeType contentType;
    private Connection connection;
    private Date date;
    private String etag;
    private String server;
    private Boolean xamzDeleteMarker;
    private String xamzRequestId;
    private String xamzVersionId;
    private String xamzExpiration;


    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public void setContentType(MimeType contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public void setEtag(String etag) {
        this.etag = etag;
    }

    @Override
    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public void setXamzDeleteMarker(Boolean xamzDeleteMarker) {
        this.xamzDeleteMarker = xamzDeleteMarker;
    }

    @Override
    public void setXamzRequestId(String xamzRequestId) {
        this.xamzRequestId = xamzRequestId;
    }

    @Override
    public void setXamzVersionId(String xamzVersionId) {
        this.xamzVersionId = xamzVersionId;
    }

    @Override
    public void setXamzExpiration(String expiration) {
        this.xamzExpiration = expiration;
    }

    public static void appendHeaderToResponse(HttpServletResponse response, S3ResponseHeaderImpl header) {
        if (header.contentLength != null)
            response.setContentLengthLong(header.contentLength);
        if (header.contentType != null)
            response.setContentType(header.contentType.toString());
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
