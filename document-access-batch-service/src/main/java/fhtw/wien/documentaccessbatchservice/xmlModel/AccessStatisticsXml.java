package fhtw.wien.documentaccessbatchservice.xmlModel;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@XmlRootElement(name = "accessStatistics")
@XmlAccessorType(XmlAccessType.FIELD)
public class AccessStatisticsXml {

    @XmlAttribute(name = "date")
    private LocalDate date;

    @XmlElement(name = "document")
    private List<DocumentAccessXml> documents;

}