package fhtw.wien.documentaccessbatchservice.batch.processor;


import fhtw.wien.documentaccessbatchservice.dto.DocumentAccessStatDto;
import fhtw.wien.documentaccessbatchservice.xmlModel.DocumentAccessXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DocumentAccessItemProcessor
        implements ItemProcessor<DocumentAccessXml, DocumentAccessStatDto>, StepExecutionListener {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentAccessItemProcessor.class);

    private LocalDate date;

    public DocumentAccessItemProcessor() {
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.date = (LocalDate) stepExecution
                .getExecutionContext()
                .get("accessDate");
        log.info("Processing access statistics for date {}", date);
    }

    @Override
    public DocumentAccessStatDto process(DocumentAccessXml item) {
        log.debug("Processing document {} with accessCount {}",
                item.getDocumentId(), item.getAccessCount());
        return new DocumentAccessStatDto(
                item.getDocumentId(),
                date,
                item.getAccessCount()
        );
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Step {} completed: read={}, written={}, skipped={}",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount());
        return stepExecution.getExitStatus();
    }

}