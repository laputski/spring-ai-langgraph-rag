package io.callicode.rag.api.controller;

import io.callicode.rag.agent.SelfRagService;
import io.callicode.rag.api.dto.ChatRequest;
import io.callicode.rag.api.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final SelfRagService selfRagService;

    public ChatController(SelfRagService selfRagService) {
        this.selfRagService = selfRagService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(selfRagService.chat(request));
    }
}
