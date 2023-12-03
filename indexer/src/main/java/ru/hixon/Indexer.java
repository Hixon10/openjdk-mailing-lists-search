package ru.hixon;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.time.temporal.ChronoUnit.SECONDS;


public class Indexer {

    /**
     * we need to limit concurrent number of HTTP requests.
     * Otherwise, we get java.util.concurrent.CompletionException: java.io.IOException: too many concurrent streams
     */
    private static final int FETCH_EMAILS_BATCH_SIZE = 50;

    private static final Duration HTTP_TIMEOUT = Duration.of(5, SECONDS);
    private static final String THREAD_SEARCH_SUBSTRING = "[ Thread ]";
    private static final int HTTP_OK_CODE = 200;
    private static final String PREFIX_EMAIL_COMMENT = "<!--";
    private static final String EMAIL_LINK_PREFIX = "<LI><A HREF=\"";
    private static final String THREAD_URL_SUFFIX = "thread.html";
    private static final String TITLE_HTML_TAG = "<TITLE>";
    private static final String B_HTML_TAG = "<B>";
    private static final String I_HTML_TAG = "<I>";
    private static final String NEXT_MESSAGE_PREFIX = "<LI>Next message (by thread): <A HREF=\"";
    private static final String PREVIOUS_MESSAGE_PREFIX = "<LI>Previous message (by thread): <A HREF=\"";
    private static final SimpleDateFormat dateParser = new SimpleDateFormat("E MMM d HH:mm:ss Z yyyy");
    private static final String HTML_SUFFIX = ".html";
    private static final String START_CONTENT_PREFIX = "<!--beginarticle-->";
    private static final String END_EMAIL_PREFIX = "<!--endarticle-->";

    private final Database database;
    private final HttpClient httpClient;
    private final List<String> mailingListArchives;

    public Indexer(Database database, HttpClient httpClient, List<String> mailingListArchives) {
        this.database = database;
        this.httpClient = httpClient;
        this.mailingListArchives = mailingListArchives;
    }

    public void index() throws Exception {
        Set<ThreadUrlInMailingList> threadUrlsForIndex = getThreadUrlsForIndex();
        for (ThreadUrlInMailingList threadUrlForIndex : threadUrlsForIndex) {
            indexMonth(threadUrlForIndex);
        }
    }

    private void indexMonth(ThreadUrlInMailingList threadUrlForIndex) throws URISyntaxException, IOException, InterruptedException, ExecutionException, ParseException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(threadUrlForIndex.getThreadUrl()))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> monthResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (monthResponse.statusCode() != HTTP_OK_CODE) {
            throw new RuntimeException("Wrong HTTP code, while getting monthPage: code=%d, threadUrl=%s".formatted(monthResponse.statusCode(), threadUrlForIndex.getThreadUrl()));
        }

        Set<String> indexedEmailsInGivenMonth = database.getIndexedEmailsInGivenMonth(threadUrlForIndex.getThreadUrl());
        List<EmailEntity> emailsForIndex = parseMonthPage(monthResponse.body(), indexedEmailsInGivenMonth, threadUrlForIndex.getThreadUrl());
        if (emailsForIndex.isEmpty()) {
            return;
        }

        List<CompletableFuture<HttpResponse<String>>> resultsCf = new ArrayList<>();
        List<CompletableFuture<HttpResponse<String>>> resultsBatchCf = new ArrayList<>();

        for (EmailEntity emailForIndex : emailsForIndex) {
            HttpRequest emailRequest = HttpRequest.newBuilder()
                    .uri(new URI(emailForIndex.getUrl()))
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();
            CompletableFuture<HttpResponse<String>> cf = httpClient.sendAsync(emailRequest, HttpResponse.BodyHandlers.ofString());
            resultsCf.add(cf);
            resultsBatchCf.add(cf);
            if (resultsBatchCf.size() == FETCH_EMAILS_BATCH_SIZE) {
                CompletableFuture.allOf(resultsBatchCf.toArray(new CompletableFuture[0])).join();
                resultsBatchCf.clear();
            }
        }
        CompletableFuture.allOf(resultsCf.toArray(new CompletableFuture[0])).join();

        for (int i = 0; i < resultsCf.size(); i++) {
            HttpResponse<String> emailPageResponse = resultsCf.get(i).get();
            EmailEntity emailForIndex = emailsForIndex.get(i);
            parseEmail(emailPageResponse.body(), emailForIndex);
        }

        database.saveEmailsForGivenMonth(emailsForIndex);
    }

    static void parseEmail(String emailPage, EmailEntity emailForIndex) throws ParseException {
        String[] lines = emailPage.split("\n");

        String emailTitle = null;
        String emailAuthor = null;
        Long timestampInSec = null;
        String nextUrl = null;
        String previousUrl = null;
        String content = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.startsWith(TITLE_HTML_TAG)) {
                emailTitle = line.substring(TITLE_HTML_TAG.length()).trim();
                continue;
            }

            if (line.startsWith(B_HTML_TAG) &&
                    ((i + 1) < lines.length) && lines[i + 1].trim().startsWith("<A HREF=") &&
                    emailAuthor == null) {
                i++;
                StringBuilder sbAuthor = new StringBuilder();
                for (int j = B_HTML_TAG.length(); j < line.length(); j++) {
                    if (line.charAt(j) == '<') {
                        break;
                    }
                    sbAuthor.append(line.charAt(j));
                }
                emailAuthor = sbAuthor.toString();
                continue;
            }

            if (line.startsWith(I_HTML_TAG) && timestampInSec == null) {
                StringBuilder sbTimestamp = new StringBuilder();
                for (int j = I_HTML_TAG.length(); j < line.length(); j++) {
                    if (line.charAt(j) == '<') {
                        break;
                    }
                    sbTimestamp.append(line.charAt(j));
                }

                timestampInSec = dateParser.parse(sbTimestamp.toString()).toInstant().getEpochSecond();
                continue;
            }

            final String monthUrlWithoutSuffix = removeThreadUrlSuffixFromUrl(emailForIndex.getMonthUrl());

            if (line.startsWith(NEXT_MESSAGE_PREFIX) && nextUrl == null) {
                StringBuilder sbNextUrl = new StringBuilder();
                for (int j = NEXT_MESSAGE_PREFIX.length(); j < line.length(); j++) {
                    if (line.charAt(j) == '.') {
                        break;
                    }
                    sbNextUrl.append(line.charAt(j));
                }

                nextUrl = monthUrlWithoutSuffix + sbNextUrl + HTML_SUFFIX;
                continue;
            }

            if (line.startsWith(PREVIOUS_MESSAGE_PREFIX) && previousUrl == null) {
                StringBuilder sbNextUrl = new StringBuilder();
                for (int j = PREVIOUS_MESSAGE_PREFIX.length(); j < line.length(); j++) {
                    if (line.charAt(j) == '.') {
                        break;
                    }
                    sbNextUrl.append(line.charAt(j));
                }

                previousUrl = monthUrlWithoutSuffix + sbNextUrl + HTML_SUFFIX;
                continue;
            }

            if (line.startsWith(START_CONTENT_PREFIX) && content == null) {
                List<String> contentSts = new ArrayList<>();
                for (int j = i + 1; j < lines.length; j++) {
                    String contentLine = lines[j].trim();
                    if (contentLine.startsWith(END_EMAIL_PREFIX)) {
                        break;
                    }
                    contentSts.add(contentLine);
                }
                content = String.join("\n", contentSts);
                continue;
            }
        }

        emailForIndex.setFields(content, emailTitle, emailAuthor, timestampInSec, previousUrl, nextUrl);
    }

    static List<EmailEntity> parseMonthPage(String monthPage, Set<String> indexedEmailsInGivenMonth, final String monthUrl) {
        String[] lines = monthPage.split("\n");
        List<EmailEntity> result = new ArrayList<>();
        Map<Integer, String> indentToReplyTo = new HashMap<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            boolean isLineWithEmailComment = line.startsWith(PREFIX_EMAIL_COMMENT) &&
                    line.length() > PREFIX_EMAIL_COMMENT.length() &&
                    Character.isDigit(line.charAt(PREFIX_EMAIL_COMMENT.length()));
            if (!isLineWithEmailComment) {
                continue;
            }

            Integer indent = Integer.parseInt(String.valueOf(line.charAt(PREFIX_EMAIL_COMMENT.length())));

            // next line is email link
            i++;
            String lineWithEmailLink = lines[i].trim();

            if (!lineWithEmailLink.startsWith(EMAIL_LINK_PREFIX)) {
                throw new RuntimeException("Unexpected lineWithEmailLink, need to check links format: %s".formatted(lineWithEmailLink));
            }
            StringBuilder emailLinkSb = new StringBuilder();
            for (int j = EMAIL_LINK_PREFIX.length(); j < lineWithEmailLink.length(); j++) {
                if (!Character.isDigit(lineWithEmailLink.charAt(j))) {
                    break;
                }
                emailLinkSb.append(lineWithEmailLink.charAt(j));
            }

            final String monthUrlWithoutSuffix = removeThreadUrlSuffixFromUrl(monthUrl);
            String emailLink = monthUrlWithoutSuffix + emailLinkSb + HTML_SUFFIX;
            indentToReplyTo.put(indent, emailLink);

            if (indexedEmailsInGivenMonth.contains(emailLink)) {
                continue;
            }

            String replyToUrl = indentToReplyTo.get(indent - 1);
            EmailEntity emailEntity = new EmailEntity(emailLink, monthUrl, indent, replyToUrl);
            result.add(emailEntity);
        }

        return result;
    }

    private static String removeThreadUrlSuffixFromUrl(final String monthUrl) {
        if (monthUrl.endsWith("/" + THREAD_URL_SUFFIX)) {
            return monthUrl.substring(0, monthUrl.length() - THREAD_URL_SUFFIX.length());
        }
        return monthUrl;
    }

    /**
     * For each mailing list define, which months should be indexed.
     * We need always reindex the current month, because it could change.
     * Apart from that, we need to index months, which we have never indexed before.
     * @return thread urls, which need to index
     */
    private Set<ThreadUrlInMailingList> getThreadUrlsForIndex() throws Exception {
        List<CompletableFuture<HttpResponse<String>>> resultsCf = new ArrayList<>();
        for (String mailingListArchive : mailingListArchives) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(mailingListArchive))
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();
            resultsCf.add(httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
        }
        CompletableFuture.allOf(resultsCf.toArray(new CompletableFuture[0])).join();

        Set<ThreadUrlInMailingList> threadUrlsForIndex = new HashSet<>();
        Set<String> indexedMonthUrls = database.getIndexedMonthUrls();

        for (int i = 0; i < resultsCf.size(); i++) {
            HttpResponse<String> archivesPageResponse = resultsCf.get(i).get();
            String currentMailingListUrl = mailingListArchives.get(i);
            if (archivesPageResponse.statusCode() != HTTP_OK_CODE) {
                throw new RuntimeException("Wrong HTTP code, while getting archivesPage: code=%d, link=%s".formatted(archivesPageResponse.statusCode(), currentMailingListUrl));
            }
            Set<ThreadUrlInMailingList> threadLinks = parseThreadLinks(archivesPageResponse.body(), indexedMonthUrls, currentMailingListUrl);
            threadUrlsForIndex.addAll(threadLinks);
        }

        return threadUrlsForIndex;
    }

    static Set<ThreadUrlInMailingList> parseThreadLinks(String archivePageContent, Set<String> indexedMonthUrls, String currentMailingListUrl) {
        int index = archivePageContent.indexOf(THREAD_SEARCH_SUBSTRING);
        boolean firstAdded = false;
        Set<ThreadUrlInMailingList> result = new HashSet<>();
        while (index >= 0) {
            StringBuilder sb = new StringBuilder();
            for (int j = index - 3; j >= 0; j--) {
                char ch = archivePageContent.charAt(j);
                if (ch == '"') {
                    break;
                }
                sb.append(ch);
            }
            String threadUrl = currentMailingListUrl + sb.reverse();
            // we want to index either the current month, or month, which we haven't seen before
            if (!firstAdded || !indexedMonthUrls.contains(threadUrl)) {
                result.add(new ThreadUrlInMailingList(threadUrl, currentMailingListUrl));
            }
            firstAdded = true;
            index = archivePageContent.indexOf(THREAD_SEARCH_SUBSTRING, index + 1);
        }
        return result;
    }
}
