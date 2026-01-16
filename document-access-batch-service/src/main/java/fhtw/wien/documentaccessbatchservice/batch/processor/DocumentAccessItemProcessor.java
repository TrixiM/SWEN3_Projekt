package fhtw.wien.documentaccessbatchservice.batch.processor;


import fhtw.wien.documentaccessbatchservice.dto.DocumentAccessStatDto;
import fhtw.wien.documentaccessbatchservice.xmlModel.DocumentAccessXml;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DocumentAccessItemProcessor
        implements ItemProcessor<DocumentAccessXml, DocumentAccessStatDto>, StepExecutionListener {

    private LocalDate date;

    public DocumentAccessItemProcessor() {
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.date = (LocalDate) stepExecution
                .getExecutionContext()
                .get("accessDate");
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