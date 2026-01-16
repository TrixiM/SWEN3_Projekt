package fhtw.wien.documentaccessbatchservice.xmlModel;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;

import java.util.UUID;
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
public class DocumentAccessXml {

    @XmlElement(name = "documentId")
    @XmlJavaTypeAdapter(UUIDAdapter.class)
    private UUID documentId;

    @XmlElement(name = "accessCount")
    private int accessCount;
}
