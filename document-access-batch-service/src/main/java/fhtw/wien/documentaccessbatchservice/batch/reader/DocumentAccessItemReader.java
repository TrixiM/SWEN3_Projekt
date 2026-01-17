package fhtw.wien.documentaccessbatchservice.batch.reader;

import fhtw.wien.documentaccessbatchservice.xmlModel.DocumentAccessXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class DocumentAccessItemReader  {
    private static final Logger log = LoggerFactory.getLogger(DocumentAccessItemReader.class);


    @Bean
    public StaxEventItemReader<DocumentAccessXml> itemReader(Jaxb2Marshaller documentUnmarshaller){
        log.info("Creating XML item reader for access log");
        return new StaxEventItemReaderBuilder<DocumentAccessXml>()
                .name("documentItemReader")
                .resource(new FileSystemResource("src/main/resources/accessLog/accessLog.xml"))
                .addFragmentRootElements("document")
                .unmarshaller(documentUnmarshaller)
                .build();
    }

    @Bean
    public Jaxb2Marshaller documentUnmarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(DocumentAccessXml.class);
        return marshaller;
    }

}
