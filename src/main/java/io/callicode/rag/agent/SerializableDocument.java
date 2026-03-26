package io.callicode.rag.agent;

import org.springframework.ai.document.Document;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializable wrapper around Spring AI's {@link Document}.
 * <p>
 * LangGraph4j 1.5.x clones state between nodes using Java {@link java.io.ObjectOutputStream},
 * which requires all state values to implement {@link Serializable}. Spring AI's
 * {@link Document} does not implement {@code Serializable}, so this wrapper carries the
 * fields we need (id, text, metadata) through the state graph, converting back to a
 * full {@link Document} only when passed to the LLM or vector store.
 * </p>
 */
public final class SerializableDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String text;
    /** Metadata values must themselves be serializable (String, Boolean, Integer, etc.). */
    private final HashMap<String, Object> metadata;

    public SerializableDocument(String id, String text, Map<String, Object> metadata) {
        this.id       = id;
        this.text     = text;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    /** Convert a Spring AI {@link Document} to a serializable form. */
    public static SerializableDocument from(Document doc) {
        return new SerializableDocument(doc.getId(), doc.getText(), doc.getMetadata());
    }

    /** Convert back to a Spring AI {@link Document} for LLM / vector store use. */
    public Document toDocument() {
        return new Document(id, text, metadata);
    }

    public String getId()                      { return id; }
    public String getText()                    { return text; }
    public HashMap<String, Object> getMetadata() { return metadata; }

    @Override
    public String toString() {
        return "SerializableDocument{id='" + id + "', text='" + text.substring(0, Math.min(text.length(), 40)) + "...'}";
    }
}
