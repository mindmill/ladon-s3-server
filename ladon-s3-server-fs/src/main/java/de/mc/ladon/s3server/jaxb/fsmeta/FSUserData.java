/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.fsmeta;


import de.mc.ladon.s3server.repository.impl.FSUser;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * @author Ralf Ulrich on 05.03.16.
 */
@XmlRootElement(name = "FsUserMeta")
public class FSUserData {

    private Map<String, FSUser> users;

    public FSUserData(Map<String, FSUser> users) {
        this.users = users;
    }

    public FSUserData() {
    }

    @XmlElement(name = "users")
    public Map<String, FSUser> getUsers() {
        return users;
    }

    public void setUsers(Map<String, FSUser> users) {
        this.users = users;
    }
}
