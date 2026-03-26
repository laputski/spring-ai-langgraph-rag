package io.callicode.rag.api.controller;

import io.callicode.rag.api.dto.DocumentIngestRequest;
import io.callicode.rag.rag.DocumentIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentIngestionService ingestionService;

    public DocumentController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/documents")
    public ResponseEntity<Map<String, Object>> ingest(@Valid @RequestBody DocumentIngestRequest request) {
        int count = ingestionService.ingestTexts(request.texts());
        return ResponseEntity.ok(Map.of(
                "ingested", count,
                "message", "Documents successfully ingested into the vector store"
        ));
    }
}
