package fhtw.wien.documentaccessbatchservice.batch.reader;

import fhtw.wien.documentaccessbatchservice.xmlModel.AccessStatisticsXml;
import fhtw.wien.documentaccessbatchservice.xmlModel.DocumentAccessXml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Iterator;
public class DocumentAccessItemReader implements ItemReader<DocumentAccessXml> {

    private AccessStatisticsXml statistics;
    private Iterator<DocumentAccessXml> iterator;

    public DocumentAccessItemReader(Resource resource) throws JAXBException {
        try (InputStream is = resource.getInputStream()) {
            JAXBContext context = JAXBContext.newInstance(AccessStatisticsXml.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            this.statistics = (AccessStatisticsXml) unmarshaller.unmarshal(is);
            this.iterator = statistics.getDocuments().iterator();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read XML resource: " + resource.getDescription(), e);
        }
    }

    @Override
    public DocumentAccessXml read() {
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    public LocalDate getDate() {
        return statistics.getDate();
    }
}
