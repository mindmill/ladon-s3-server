/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

/**
 * @author Ralf Ulrich on 17.02.16.
 */
@XmlSchema(namespace = "http://s3.amazonaws.com/doc/2006-03-01/",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {@XmlNs(namespaceURI = "http://s3.amazonaws.com/doc/2006-03-01/", prefix = "")})
package de.mc.ladon.s3server.jaxb.entities;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;

