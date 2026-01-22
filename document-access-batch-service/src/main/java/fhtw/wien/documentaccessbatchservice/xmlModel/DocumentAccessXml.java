package fhtw.wien.documentaccessbatchservice.xmlModel;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;

import java.util.UUID;
@Getter
@XmlAccessorType(XmlAccessType.FIELD) //FIELD: JAXB ignores getter & setters
@XmlRootElement(name = "document") //<document> maps into this class, must match with config in reader
public class DocumentAccessXml {

    @XmlElement(name = "documentId") //<documentId>
    @XmlJavaTypeAdapter(UUIDAdapter.class) //to unmarshall string into UUID
    private UUID documentId;

    @XmlElement(name = "accessCount")//<accessCount>
    private int accessCount; //if accessCount empty then 0
}
