package fhtw.wien.documentaccessbatchservice.xmlModel;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@XmlRootElement(name = "accessStatistics")
@XmlAccessorType(XmlAccessType.FIELD)
public class AccessStatisticsXml { //not used

    @XmlAttribute(name = "date")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate date;

    @XmlElement(name = "document")
    private List<DocumentAccessXml> documents;
}
