package ru.hixon;

import java.util.Objects;

/**
 * Representation of email in database
 */
public class EmailEntity {

    private final String url;
    private final String monthUrl;
    private final Integer indent;
    private final String replyToUrl;

    private String content;
    private String emailTitle;
    private String emailAuthor;
    private Long timestampInSec;
    private String previousUrl;
    private String nextUrl;

    public EmailEntity(String url, String monthUrl, Integer indent, String replyToUrl) {
        this.url = Objects.requireNonNull(url, "url");
        this.monthUrl = Objects.requireNonNull(monthUrl, "monthUrl");
        this.indent = Objects.requireNonNull(indent, "indent");
        this.replyToUrl = replyToUrl;
    }

    public void setFields(String content,
                          String emailTitle,
                          String emailAuthor,
                          Long timestampInSec,
                          String previousUrl,
                          String nextUrl) {

        this.content = Objects.requireNonNull(content, "content");
        this.emailTitle = Objects.requireNonNull(emailTitle, "emailTitle");
        this.emailAuthor = Objects.requireNonNull(emailAuthor, "emailAuthor");
        this.timestampInSec = Objects.requireNonNull(timestampInSec, "timestampInSec");
        this.previousUrl = previousUrl;
        this.nextUrl = nextUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getMonthUrl() {
        return monthUrl;
    }

    public Integer getIndent() {
        return indent;
    }

    public String getReplyToUrl() {
        return replyToUrl;
    }

    public String getContent() {
        return content;
    }

    public String getEmailTitle() {
        return emailTitle;
    }

    public String getEmailAuthor() {
        return emailAuthor;
    }

    public Long getTimestampInSec() {
        return timestampInSec;
    }

    public String getPreviousUrl() {
        return previousUrl;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailEntity that = (EmailEntity) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "EmailEntity{" +
                "url='" + url + '\'' +
                ", monthUrl='" + monthUrl + '\'' +
                ", indent=" + indent +
                ", replyToUrl='" + replyToUrl + '\'' +
                ", content='" + content + '\'' +
                ", emailTitle='" + emailTitle + '\'' +
                ", emailAuthor='" + emailAuthor + '\'' +
                ", timestampInSec=" + timestampInSec +
                ", previousUrl='" + previousUrl + '\'' +
                ", nextUrl='" + nextUrl + '\'' +
                '}';
    }
}
