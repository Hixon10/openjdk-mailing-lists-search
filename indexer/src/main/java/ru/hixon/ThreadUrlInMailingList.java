package ru.hixon;

import java.util.Objects;

public class ThreadUrlInMailingList {
    private final String threadUrl;
    private final String mailingListUrl;

    public ThreadUrlInMailingList(String threadUrl, String mailingListUrl) {
        this.threadUrl = threadUrl;
        this.mailingListUrl = mailingListUrl;
    }

    public String getMailingListUrl() {
        return mailingListUrl;
    }

    public String getThreadUrl() {
        return threadUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadUrlInMailingList that = (ThreadUrlInMailingList) o;
        return threadUrl.equals(that.threadUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threadUrl);
    }

    @Override
    public String toString() {
        return "ThreadUrlInMailingList[" +
                "threadUrl=" + threadUrl + ", " +
                "mailingListUrl=" + mailingListUrl + ']';
    }

}
