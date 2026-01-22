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

    @Value("${app.accesslog.dir}") //where xml files are read from
    private String accessLogDir;

    @Value("${app.accesslog.archive.dir}")//where processed files are archived
    private String archiveDir;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        File sourceDir = new File(accessLogDir); //source dir
        File archive = new File(archiveDir); //target/archive dir

        if (!archive.exists()) {
            archive.mkdirs();
        }

        File[] xmlFiles = sourceDir.listFiles((dir, name) -> name.endsWith(".xml"));//filters for files ending w .xml

        //move .xml files to archive
        if (xmlFiles != null) {
            for (File file : xmlFiles) {
                File dest = new File(archive, file.getName()); //constructs destination
                Files.move(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING); //move from source to archive, overwrite existing file if present
            }
        }

        return RepeatStatus.FINISHED;
    }
}
