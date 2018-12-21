package org.alex73.voice.splitter.storage;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
@XmlRootElement
public class Parts {
    @XmlElement(name = "audio")
    private List<Audio> files;

    public List<Audio> getFiles() {
        if (files == null) {
            files = new ArrayList<>();
        }
        return files;
    }
}
