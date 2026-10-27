/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.entities;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Request body of a bulk delete request ({@code POST /bucket?delete}).
 *
 * <pre>{@code
 * <Delete>
 *   <Object><Key>key1</Key></Object>
 *   <Object><Key>key2</Key><VersionId>abc</VersionId></Object>
 * </Delete>
 * }</pre>
 */
@XmlRootElement(name = "Delete")
public class Delete {

    private List<DeleteObject> objects = new ArrayList<>();

    public List<DeleteObject> getObjects() {
        return objects;
    }

    public void setObjects(List<DeleteObject> objects) {
        this.objects = objects;
    }

    @XmlRootElement
    public static class DeleteObject {
        private String key;
        private String versionId;

        @XmlElement(name = "Key")
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @XmlElement(name = "VersionId", required = false)
        public String getVersionId() {
            return versionId;
        }

        public void setVersionId(String versionId) {
            this.versionId = versionId;
        }
    }
}
