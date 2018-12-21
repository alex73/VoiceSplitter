package org.alex73.voice.splitter.storage;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlType
public class Part {
    @XmlAttribute
    public String startTime;
    @XmlValue
    public String text;
}
