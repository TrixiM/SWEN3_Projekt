package fhtw.wien.documentaccessbatchservice.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batch")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job documentAccessJob;

    public BatchController(JobLauncher jobLauncher, Job documentAccessJob) {
        this.jobLauncher = jobLauncher;
        this.documentAccessJob = documentAccessJob;
    }

    @PostMapping("/run")
    public String runJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(documentAccessJob, params);
        return "Job started";
    }
}
