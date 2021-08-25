/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.entities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
@XmlRootElement(name = "CommonPrefixes")
public class CommonPrefixes {

    private String prefix;


    public CommonPrefixes() {
    }

    public CommonPrefixes(String prefix) {
        this.prefix = prefix;
    }

    @XmlElement(name = "Prefix")
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
