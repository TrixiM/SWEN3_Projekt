package fhtw.wien.documentaccessbatchservice.batch.writer;

import fhtw.wien.documentaccessbatchservice.messaging.DocumentAccessStatProducer;
import fhtw.wien.documentaccessbatchservice.dto.DocumentAccessStatDto;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class DocumentAccessItemWriter implements ItemWriter<DocumentAccessStatDto> {

    private final DocumentAccessStatProducer producer;

    public DocumentAccessItemWriter(DocumentAccessStatProducer producer) {
        this.producer = producer;
    }

    @Override
    public void write(Chunk<? extends DocumentAccessStatDto> chunk) throws Exception {
        for (DocumentAccessStatDto dto : chunk) {
            producer.send(dto);
        }
    }
}