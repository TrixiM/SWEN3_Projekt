package fhtw.wien.documentaccessbatchservice.xmlModel;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;

import java.util.UUID;
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
public class DocumentAccessXml {

    private UUID documentId;
    private int accessCount;

}