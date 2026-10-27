/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.entities;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Response of a bulk delete request ({@code POST /bucket?delete}).
 * Successful deletions are reported in {@code <Deleted>} entries, per-key failures
 * in {@code <Error>} entries. The whole request still returns HTTP 200 (like S3).
 */
@XmlRootElement(name = "DeleteResult")
public class DeleteResult {

    private List<Deleted> deleted = new ArrayList<>();
    private List<DeleteError> errors = new ArrayList<>();

    public List<Deleted> getDeleted() {
        return deleted;
    }

    public void setDeleted(List<Deleted> deleted) {
        this.deleted = deleted;
    }

    public List<DeleteError> getErrors() {
        return errors;
    }

    public void setErrors(List<DeleteError> errors) {
        this.errors = errors;
    }

    @XmlRootElement
    public static class Deleted {
        private String key;
        private Boolean deleteMarker;
        private String versionId;

        public Deleted() {
        }

        public Deleted(String key) {
            this.key = key;
        }

        @XmlElement(name = "Key")
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @XmlElement(name = "DeleteMarker", required = false)
        public Boolean getDeleteMarker() {
            return deleteMarker;
        }

        public void setDeleteMarker(Boolean deleteMarker) {
            this.deleteMarker = deleteMarker;
        }

        @XmlElement(name = "VersionId", required = false)
        public String getVersionId() {
            return versionId;
        }

        public void setVersionId(String versionId) {
            this.versionId = versionId;
        }
    }

    @XmlRootElement
    public static class DeleteError {
        private String key;
        private String code;
        private String message;

        public DeleteError() {
        }

        public DeleteError(String key, String code, String message) {
            this.key = key;
            this.code = code;
            this.message = message;
        }

        @XmlElement(name = "Key")
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @XmlElement(name = "Code")
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        @XmlElement(name = "Message")
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
