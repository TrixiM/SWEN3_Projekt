package fhtw.wien.documentaccessbatchservice.scheduler;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class BatchScheduler {
    private final JobLauncher jobLauncher;
    private final Job documentAccessJob;

    public BatchScheduler(JobLauncher jobLauncher,
                          Job documentAccessJob) {
        this.jobLauncher = jobLauncher;
        this.documentAccessJob = documentAccessJob;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void runJob() throws Exception {
        jobLauncher.run(
                documentAccessJob,
                new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters()
        );
    }
}
