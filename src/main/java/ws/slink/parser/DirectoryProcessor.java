package ws.slink.parser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ws.slink.config.AppConfig;
import ws.slink.model.ProcessingResult;
import ws.slink.zendesk.ZendeskHierarchy;
import ws.slink.zendesk.ZendeskTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static ws.slink.model.ProcessingResult.ResultType.RT_PUB_FAILURE;

@Slf4j
@Component
@RequiredArgsConstructor (onConstructor = @__(@Autowired))
public class DirectoryProcessor {

    private final @NonNull FileProcessor fileProcessor;
    private final @NonNull AppConfig appConfig;
    private final @NonNull ZendeskTools zendeskTools;

    public ProcessingResult process(String directoryPath, ZendeskHierarchy hierarchy) {
        log.trace("processing: {}", directoryPath);
        ProcessingResult result = new ProcessingResult();

        if (!zendeskTools.updateHierarchy(hierarchy, readProperties(directoryPath))) {
            log.warn("could not load zendesk hierarchy data");
        }

        result.merge(processAllDirectories(directoryPath, hierarchy));
        result.merge(processAllFiles(directoryPath, hierarchy));

        return result;
    }

    private ProcessingResult processAllFiles(String directoryPath, ZendeskHierarchy hierarchy) {
        try {
            ProcessingResult result = new ProcessingResult();
            Files.list(Paths.get(directoryPath))
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".adoc") || f.getName().endsWith(".asciidoc"))
                .map(f -> f.getAbsolutePath())
                .parallel()
                .forEach(f -> result.merge(fileProcessor.process(f, hierarchy)));
            return result;
        } catch (IOException e) {
            log.error("error processing files in {}: {}", directoryPath, e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
            return new ProcessingResult(RT_PUB_FAILURE);
        }
    }

    private ProcessingResult processAllDirectories(String directoryPath, ZendeskHierarchy hierarchy) {
        try {
            ProcessingResult result = new ProcessingResult();
            Files.list(Paths.get(directoryPath))
                .map(Path::toFile)
                .filter(f -> f.isDirectory())
                .map(File::toPath)
                .map(Path::toString)
                .parallel()
                .forEach(d -> result.merge(process(d, hierarchy)));
            return result;
        } catch (IOException e) {
            log.error("error processing directory {}: {}", directoryPath, e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
            return new ProcessingResult(RT_PUB_FAILURE);
        }
    }

    private Properties readProperties(String directoryPath) {
        String filename = directoryPath + File.separator + appConfig.getConfigFileName();
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(filename)) {
            properties.load(input);
        } catch (IOException ex) {
            log.error("Error reading properties from '{}': {}", filename, ex.getMessage());
            if (log.isTraceEnabled())
                ex.printStackTrace();
        }
        return properties;
    }

}


//        properties.putAll(readProperties(directoryPath));
//        System.out.println("-----------------------------------------------------------");
//        System.out.println("properties at " + directoryPath);
//        properties.keySet().stream().forEach(k -> System.out.println(String.format("%20s: %s", k, properties.getProperty((String)k))));

//        result.merge(removeStaleDocuments(directoryPath));

//    private ProcessingResult removeStaleDocuments(String directoryPath) {
//        return new ProcessingResult();
//        try {
//            ProcessingResult result = new ProcessingResult();
//            List<Document> repositoryDocuments =
//                Files.list(Paths.get(directoryPath))
//                    .parallel()
//                    .map(Path::toFile)
//                    .filter(File::isFile)
//                    .filter(f -> f.getName().endsWith(".adoc") || f.getName().endsWith(".asciidoc"))
//                    .map(f -> f.getAbsolutePath())
//                    .map(f -> fileProcessor.read(f))
//                    .filter(f -> f.isPresent())
//                    .map(f -> f.get())
//                    .collect(Collectors.toList())
//            ;
//
//            return result;
//        } catch (IOException e) {
//            log.error("error removing stale documents from {}: {}", directoryPath, e.getMessage());
//            if (log.isTraceEnabled())
//                e.printStackTrace();
//            return ProcessingResult.PUBLICATION_FAILURE;
//        }
//    }

