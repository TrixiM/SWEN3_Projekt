package fhtw.wien.documentaccessbatchservice.config;

import fhtw.wien.documentaccessbatchservice.batch.ArchiveTasklet;
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

    //defines batch process
    @Bean
    public Job documentAccessJob(JobRepository jobRepository, //to store job metadata (status, executions,..)
                                 Step documentAccessStep,
                                 Step archiveStep) {
        return new JobBuilder("documentAccessJob", jobRepository) //creates a job with name
                .incrementer(new RunIdIncrementer()) //allows jobs to run w unique IDs
                .start(documentAccessStep) //main processing step
                .next(archiveStep) //additional step to archive (only if main step is successful), runs once per job
                .build();
    }


    @Bean
    public Step documentAccessStep(JobRepository jobRepository,
                                   PlatformTransactionManager txManager,
                                   MultiResourceItemReader<DocumentAccessXml> multiResourceItemReader,
                                   DocumentAccessItemProcessor processor,
                                   DocumentAccessItemWriter writer) throws JAXBException {

        return new StepBuilder("documentAccessStep", jobRepository)
                .<DocumentAccessXml, DocumentAccessStatDto>chunk(10, txManager) //Input: DocumentAccessXml, Output: DocumentAccessDto, reads 10 inputs in one go
                .reader(multiResourceItemReader)
                .processor(processor)
                .writer(writer)
                .listener(processor)
                .build();
    }

    @Bean
    public Step archiveStep(JobRepository jobRepository, PlatformTransactionManager txManager, ArchiveTasklet archiveTasklet) {
        return new StepBuilder("archiveStep", jobRepository)
                .tasklet(archiveTasklet, txManager)
                .build();
    }
}
