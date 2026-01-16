package fhtw.wien.documentaccessbatchservice.xmlModel;

import jakarta.xml.bind.JAXBException;
import lombok.Getter;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.core.io.Resource;

import jakarta.xml.bind.JAXBContext;

import java.io.IOException;
import java.time.LocalDate;

public class AccessStatisticsDateListener implements StepExecutionListener {

    private final Resource xmlResource;
    @Getter
    private LocalDate date;

    public AccessStatisticsDateListener(Resource xmlResource) {
        this.xmlResource = xmlResource;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        JAXBContext context = null;
        try {
            context = JAXBContext.newInstance(AccessStatisticsXml.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        AccessStatisticsXml stats =
                null;
        try {
            stats = (AccessStatisticsXml) context.createUnmarshaller()
                    .unmarshal(xmlResource.getInputStream());
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }

        this.date = stats.getDate();
        stepExecution.getExecutionContext().put("accessDate", date);
    }

}

