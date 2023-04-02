/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.fsmeta;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * @author Ralf Ulrich on 04.03.16.
 */
@XmlRootElement(name = "FsMeta")
public class FSStorageMeta {

    private Map<String, String> meta;

    public FSStorageMeta() {
    }

    public FSStorageMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    @XmlElement(name = "Data")
    @XmlElementWrapper(name = "Entries")
    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }
}
