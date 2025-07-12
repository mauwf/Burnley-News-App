package com.example.burnleynews;

import java.util.Date;
import java.util.Objects;

/**
 * A simple data class (POJO) to hold the information for a single news article.
 */
public class NewsArticle {
    private String title;
    private String link;
    private String description;
    private Date pubDate;
    private String source;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    // Overriding equals and hashCode is necessary for DiffUtil to correctly
    // detect when the content of an item has changed.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsArticle that = (NewsArticle) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(link, that.link) &&
                Objects.equals(description, that.description) &&
                Objects.equals(pubDate, that.pubDate) &&
                Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, link, description, pubDate, source);
    }
}
