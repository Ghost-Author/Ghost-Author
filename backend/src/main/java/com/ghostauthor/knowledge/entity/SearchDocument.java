package com.ghostauthor.knowledge.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "knowledge_docs")
public class SearchDocument {

    @Id
    private String id;

    private String slug;

    private String title;

    private String summary;

    private String content;

    public SearchDocument() {
    }

    public SearchDocument(String id, String slug, String title, String summary, String content) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
