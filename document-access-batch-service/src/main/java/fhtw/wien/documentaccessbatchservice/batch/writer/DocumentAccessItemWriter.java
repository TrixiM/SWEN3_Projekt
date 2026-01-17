package fhtw.wien.documentaccessbatchservice.batch.writer;

import fhtw.wien.documentaccessbatchservice.messaging.DocumentAccessStatProducer;
import fhtw.wien.documentaccessbatchservice.dto.DocumentAccessStatDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class DocumentAccessItemWriter implements ItemWriter<DocumentAccessStatDto> {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentAccessItemWriter.class);


    private final DocumentAccessStatProducer producer;

    public DocumentAccessItemWriter(DocumentAccessStatProducer producer) {
        this.producer = producer;
    }

    @Override
    public void write(Chunk<? extends DocumentAccessStatDto> chunk) {
        log.info("Writing chunk of {} document access records", chunk.size());
        for (DocumentAccessStatDto dto : chunk) {
            producer.send(dto);
        }
    }
}