package io.callicode.rag.cache;

import java.io.Serializable;

public class CacheEntry implements Serializable {

    private String query;
    private float[] embedding;
    private String answer;

    public CacheEntry() {}

    public CacheEntry(String query, float[] embedding, String answer) {
        this.query = query;
        this.embedding = embedding;
        this.answer = answer;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
}
