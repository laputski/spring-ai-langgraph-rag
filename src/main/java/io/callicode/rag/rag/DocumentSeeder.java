package io.callicode.rag.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the Qdrant vector store with sample technical documentation on startup.
 * <p>
 * Runs only when {@code app.seeder.enabled=true} (the default).
 * Disable in tests by setting {@code app.seeder.enabled=false}.
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true", matchIfMissing = true)
public class DocumentSeeder implements ApplicationRunner {

    private final DocumentIngestionService ingestionService;

    private static final List<String> SAMPLE_DOCS = List.of(
            "data/tech-docs/spring-boot-overview.txt",
            "data/tech-docs/docker-compose-guide.txt",
            "data/tech-docs/java-virtual-threads.txt",
            "data/tech-docs/vector-databases-explained.txt",
            "data/tech-docs/kubernetes-basics.txt"
    );

    public DocumentSeeder(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Seeding sample tech documents into Qdrant...");
        SAMPLE_DOCS.forEach(path -> ingestionService.ingestResource(new ClassPathResource(path)));
        log.info("Seeding complete.");
    }
}
