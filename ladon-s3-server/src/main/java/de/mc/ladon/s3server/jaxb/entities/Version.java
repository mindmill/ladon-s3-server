package de.mc.ladon.s3server.jaxb.entities;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * @author Ralf Ulrich
 *         30.10.16
 */
@XmlRootElement(name = "Version")
public class Version extends AbstractVersionElement {

    public Version() {
        super();
    }

    public Version(Owner owner, String key, String versionId, boolean isLatest, Date lastModified, String etag, Long size, String storageClass) {
        super(owner, key, versionId, isLatest, lastModified, etag, size, storageClass);
    }
}
