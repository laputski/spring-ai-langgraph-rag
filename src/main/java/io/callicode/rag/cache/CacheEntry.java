package io.callicode.rag.cache;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class CacheEntry implements Serializable {

    private String query;
    private float[] embedding;
    private String answer;

    public CacheEntry(String query, float[] embedding, String answer) {
        this.query = query;
        this.embedding = embedding;
        this.answer = answer;
    }
}
