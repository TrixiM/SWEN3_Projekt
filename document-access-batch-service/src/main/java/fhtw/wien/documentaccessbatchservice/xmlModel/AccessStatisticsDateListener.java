package fhtw.wien.documentaccessbatchservice.xmlModel;

import fhtw.wien.documentaccessbatchservice.batch.reader.ResourceAwareStaxReader;
import jakarta.xml.bind.JAXBException;
import lombok.Getter;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ResourceAware;
import org.springframework.core.io.Resource;

import jakarta.xml.bind.JAXBContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;

public class AccessStatisticsDateListener implements ItemReadListener<DocumentAccessXml> {

    private final ResourceAwareStaxReader<DocumentAccessXml> reader;
    private StepExecution stepExecution;
    private Resource lastResource;

    public AccessStatisticsDateListener(ResourceAwareStaxReader<DocumentAccessXml> reader) {
        this.reader = reader;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public void beforeRead() {
        Resource resource = reader.getResource();
        if (resource != null && !resource.equals(lastResource)) {
            lastResource = resource;

            try {
                JAXBContext context = JAXBContext.newInstance(AccessStatisticsXml.class);
                AccessStatisticsXml stats =
                        (AccessStatisticsXml) context
                                .createUnmarshaller()
                                .unmarshal(resource.getInputStream());

                stepExecution.getExecutionContext().put("accessDate", stats.getDate());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read access date from " + resource.getFilename(), e);
            }
        }
    }

    @Override
    public void afterRead(DocumentAccessXml item) { }

    @Override
    public void onReadError(Exception ex) { }
}
