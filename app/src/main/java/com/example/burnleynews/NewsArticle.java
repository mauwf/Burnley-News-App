package com.example.burnleynews;

import java.util.Date;

/**
 * A simple data class (POJO) to hold the information for a single news article.
 */
public class NewsArticle {
    private String title;
    private String link;
    private String description;
    private Date pubDate; // Field to store the publication date
    private String source; // Field to store the news source

    // Getter for the article title
    public String getTitle() {
        return title;
    }

    // Setter for the article title
    public void setTitle(String title) {
        this.title = title;
    }

    // Getter for the article link
    public String getLink() {
        return link;
    }

    // Setter for the article link
    public void setLink(String link) {
        this.link = link;
    }

    // Getter for the article description
    public String getDescription() {
        return description;
    }

    // Setter for the article description
    public void setDescription(String description) {
        this.description = description;
    }

    // Getter for the publication date
    public Date getPubDate() {
        return pubDate;
    }

    // Setter for the publication date
    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    // Getter for the source
    public String getSource() {
        return source;
    }

    // Setter for the source
    public void setSource(String source) {
        this.source = source;
    }
}
