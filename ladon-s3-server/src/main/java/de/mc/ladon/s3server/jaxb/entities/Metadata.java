package de.mc.ladon.s3server.jaxb.entities;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@XmlRootElement(name = "Metadata")
public class Metadata extends HashMap<String, String> {
    public Metadata() {
        super();
    }

    public Metadata(Map<? extends String, ? extends String> map) {
        super(map);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return super.entrySet();
    }
}
