package fhtw.wien.documentaccessbatchservice.batch;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Component
public class ArchiveTasklet implements Tasklet {

    @Value("${app.accesslog.dir}")
    private String accessLogDir;

    @Value("${app.accesslog.archive.dir}")
    private String archiveDir;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        File sourceDir = new File(accessLogDir);
        File archive = new File(archiveDir);

        if (!archive.exists()) {
            archive.mkdirs();
        }

        File[] xmlFiles = sourceDir.listFiles((dir, name) -> name.endsWith(".xml"));

        if (xmlFiles != null) {
            for (File file : xmlFiles) {
                File dest = new File(archive, file.getName());
                Files.move(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return RepeatStatus.FINISHED;
    }
}
