package io.callicode.rag.agent;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SelfRagState extends AgentState {

    public static final String QUERY            = "query";
    public static final String REWRITTEN_QUERY  = "rewrittenQuery";
    public static final String SEMANTIC_DOCS    = "semanticDocs";
    public static final String KEYWORD_DOCS     = "keywordDocs";
    public static final String DOCUMENTS        = "documents";
    public static final String ANSWER           = "answer";
    public static final String RETRY_COUNT      = "retryCount";
    public static final String BLOCKED          = "blocked";
    public static final String BLOCK_REASON     = "blockReason";
    public static final String CACHE_HIT        = "cacheHit";
    public static final String SESSION_ID       = "sessionId";

    // Map.ofEntries used because Map.of() supports at most 10 key-value pairs
    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry(QUERY,           Channel.of(() -> "")),
            Map.entry(REWRITTEN_QUERY, Channel.of(() -> "")),
            Map.entry(SEMANTIC_DOCS,   Channel.of(ArrayList::new)),
            Map.entry(KEYWORD_DOCS,    Channel.of(ArrayList::new)),
            Map.entry(DOCUMENTS,       Channel.of(ArrayList::new)),
            Map.entry(ANSWER,          Channel.of(() -> "")),
            Map.entry(RETRY_COUNT,     Channel.of(() -> 0)),
            Map.entry(BLOCKED,         Channel.of(() -> false)),
            Map.entry(BLOCK_REASON,    Channel.of(() -> "")),
            Map.entry(CACHE_HIT,       Channel.of(() -> false)),
            Map.entry(SESSION_ID,      Channel.of(() -> ""))
    );

    public SelfRagState(Map<String, Object> initData) {
        super(initData);
    }

    public String query() {
        return this.<String>value(QUERY).orElse("");
    }

    public String rewrittenQuery() {
        return this.<String>value(REWRITTEN_QUERY).orElse("");
    }

    /** Returns rewritten query if present, otherwise original query. */
    public String effectiveQuery() {
        String rq = rewrittenQuery();
        return (rq == null || rq.isBlank()) ? query() : rq;
    }

    @SuppressWarnings("unchecked")
    public List<SerializableDocument> semanticDocs() {
        return this.<List<SerializableDocument>>value(SEMANTIC_DOCS).orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<SerializableDocument> keywordDocs() {
        return this.<List<SerializableDocument>>value(KEYWORD_DOCS).orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<SerializableDocument> documents() {
        return this.<List<SerializableDocument>>value(DOCUMENTS).orElse(List.of());
    }

    public String answer() {
        return this.<String>value(ANSWER).orElse("");
    }

    public int retryCount() {
        return this.<Integer>value(RETRY_COUNT).orElse(0);
    }

    public boolean isBlocked() {
        return this.<Boolean>value(BLOCKED).orElse(false);
    }

    public String blockReason() {
        return this.<String>value(BLOCK_REASON).orElse("");
    }

    public boolean isCacheHit() {
        return this.<Boolean>value(CACHE_HIT).orElse(false);
    }
}
