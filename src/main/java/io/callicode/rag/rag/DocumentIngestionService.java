package io.callicode.rag.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Ingests text content into the Qdrant vector store.
 * Splits documents using Spring AI's {@link TokenTextSplitter} before embedding.
 */
@Slf4j
@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.splitter = new TokenTextSplitter();
    }

    /**
     * Ingests a list of raw text strings.
     *
     * @return number of chunks stored
     */
    public int ingestTexts(List<String> texts) {
        List<Document> docs = texts.stream().map(Document::new).toList();
        List<Document> chunks = splitter.apply(docs);
        vectorStore.add(chunks);
        log.info("Ingested {} chunks from {} text(s)", chunks.size(), texts.size());
        return chunks.size();
    }

    /**
     * Ingests a classpath resource (plain text file).
     */
    public void ingestResource(Resource resource) {
        try {
            String text = resource.getContentAsString(StandardCharsets.UTF_8);
            ingestTexts(List.of(text));
            log.info("Ingested resource: {}", resource.getFilename());
        } catch (IOException e) {
            log.error("Failed to ingest resource {}: {}", resource.getFilename(), e.getMessage());
        }
    }
}
