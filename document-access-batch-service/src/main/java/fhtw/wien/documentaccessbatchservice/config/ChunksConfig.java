package fhtw.wien.documentaccessbatchservice.config;

import fhtw.wien.documentaccessbatchservice.batch.processor.DocumentAccessItemProcessor;
import fhtw.wien.documentaccessbatchservice.batch.writer.DocumentAccessItemWriter;
import fhtw.wien.documentaccessbatchservice.dto.DocumentAccessStatDto;
import fhtw.wien.documentaccessbatchservice.xmlModel.DocumentAccessXml;
import jakarta.xml.bind.JAXBException;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ChunksConfig {

    @Bean
    public Job documentAccessJob(JobRepository jobRepository,
                                 Step documentAccessStep) {
        return new JobBuilder("documentAccessJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(documentAccessStep)
                .build();
    }

    @Bean
    public Step documentAccessStep(JobRepository jobRepository,
                                   PlatformTransactionManager txManager,
                                   MultiResourceItemReader<DocumentAccessXml> multiResourceItemReader,
                                   DocumentAccessItemProcessor processor,
                                   DocumentAccessItemWriter writer) throws JAXBException {

        return new StepBuilder("documentAccessStep", jobRepository)
                .<DocumentAccessXml, DocumentAccessStatDto>chunk(10, txManager)
                .reader(multiResourceItemReader)
                .processor(processor)
                .writer(writer)
                .listener(processor)
                .build();
    }
}
