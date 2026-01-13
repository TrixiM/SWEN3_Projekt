package fhtw.wien.documentaccessbatchservice.batch.processor;


import fhtw.wien.documentaccessbatchservice.dto.DocumentAccessStatDto;
import fhtw.wien.documentaccessbatchservice.xmlModel.DocumentAccessXml;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;

public class DocumentAccessItemProcessor
        implements ItemProcessor<DocumentAccessXml, DocumentAccessStatDto> {

    private LocalDate date;

    public DocumentAccessItemProcessor() {
    }

    @Override
    public DocumentAccessStatDto process(DocumentAccessXml item) {
        return new DocumentAccessStatDto(
                item.getDocumentId(),
                date,
                item.getAccessCount()
        );
    }
}