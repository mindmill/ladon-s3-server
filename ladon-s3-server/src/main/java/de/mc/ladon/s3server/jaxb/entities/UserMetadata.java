package de.mc.ladon.s3server.jaxb.entities;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@XmlRootElement(name = "UserMetadata")
public class UserMetadata extends HashMap<String, String> {
    public UserMetadata() {
        super();
    }

    public UserMetadata(Map<? extends String, ? extends String> map) {
        super(map);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return super.entrySet();
    }
}
