package de.mc.ladon.s3server.jaxb.entities;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * @author Ralf Ulrich
 *         30.10.16
 */
@XmlRootElement(name = "VersionSummary")
public class VersionSummary extends AbstractVersionSummary {

    public VersionSummary() {
        super();
    }

    public VersionSummary(Owner owner, String key, String versionId, boolean isLatest, Date lastModified, String etag, Long size, String storageClass) {
        super(owner, key, versionId, isLatest, lastModified, etag, size, storageClass);
    }
}
