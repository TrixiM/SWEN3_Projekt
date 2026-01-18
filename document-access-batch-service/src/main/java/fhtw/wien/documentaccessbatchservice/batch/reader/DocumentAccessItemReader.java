package fhtw.wien.documentaccessbatchservice.batch.reader;

import fhtw.wien.documentaccessbatchservice.xmlModel.DocumentAccessXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class DocumentAccessItemReader  {
    private static final Logger log = LoggerFactory.getLogger(DocumentAccessItemReader.class);

    @Bean
    public MultiResourceItemReader<DocumentAccessXml> multiResourceItemReader(
            StaxEventItemReader<DocumentAccessXml> documentXmlReader) throws Exception {

        log.info("Creating MultiResourceItemReader for access logs");

        MultiResourceItemReader<DocumentAccessXml> reader =
                new MultiResourceItemReader<>();

        reader.setResources(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:accessLog/*.xml")
        );

        reader.setDelegate(documentXmlReader);
        reader.setStrict(true);

        return reader;
    }

    /*@Bean
    public ResourceAwareStaxReader<DocumentAccessXml> documentAccessXmlReader(Jaxb2Marshaller documentUnmarshaller) {
        ResourceAwareStaxReader<DocumentAccessXml> reader = new ResourceAwareStaxReader<>();

        reader.setName("documentItemReader");
        reader.setFragmentRootElementName("document");
        reader.setUnmarshaller(documentUnmarshaller);

        return reader;
    }*/



    @Bean
    public StaxEventItemReader<DocumentAccessXml> documentAccessXmlReader(Jaxb2Marshaller documentUnmarshaller){
        log.info("Creating XML item reader for access log");
        return new StaxEventItemReaderBuilder<DocumentAccessXml>()
                .name("documentItemReader")
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
