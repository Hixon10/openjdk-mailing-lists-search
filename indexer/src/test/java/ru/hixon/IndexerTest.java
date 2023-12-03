package ru.hixon;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;

public class IndexerTest {

    @Test
    public void canInitLogsTest() throws IOException {
        InputStream stream = Main.class.getResourceAsStream("/logging.properties");
        String conf = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        Assertions.assertTrue(conf.contains("handlers= java.util.logging.ConsoleHandler"));
        LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(conf.getBytes()));
    }

    @Test
    public void parseThreadLinksTest() throws IOException {
        InputStream contentIs = getClass().getResourceAsStream("/archivesPage.html");
        String html = new String(contentIs.readAllBytes(), StandardCharsets.UTF_8);
        Set<ThreadUrlInMailingList> threadUrls = Indexer.parseThreadLinks(html, Set.of(), "https://mail.openjdk.org/pipermail/valhalla-dev/");

        Assertions.assertEquals(98, threadUrls.size());
        for (ThreadUrlInMailingList threadUrl : threadUrls) {
            Assertions.assertEquals("https://mail.openjdk.org/pipermail/valhalla-dev/", threadUrl.getMailingListUrl());
        }
        Assertions.assertTrue(threadUrls.stream().anyMatch(p -> p.getThreadUrl().equals("https://mail.openjdk.org/pipermail/valhalla-dev/2022-September/thread.html")));
        Assertions.assertTrue(threadUrls.stream().anyMatch(p -> p.getThreadUrl().equals("https://mail.openjdk.org/pipermail/valhalla-dev/2021-August/thread.html")));
        Assertions.assertTrue(threadUrls.stream().anyMatch(p -> p.getThreadUrl().equals("https://mail.openjdk.org/pipermail/valhalla-dev/2021-May/thread.html")));
        Assertions.assertTrue(threadUrls.stream().anyMatch(p -> p.getThreadUrl().equals("https://mail.openjdk.org/pipermail/valhalla-dev/2014-July/thread.html")));

        // with filters
        threadUrls = Indexer.parseThreadLinks(html, Set.of(
                "https://mail.openjdk.org/pipermail/valhalla-dev/2022-September/thread.html",
                "https://mail.openjdk.org/pipermail/valhalla-dev/2021-May/thread.html"
        ), "https://mail.openjdk.org/pipermail/valhalla-dev/");

        Assertions.assertEquals(97, threadUrls.size());
        for (ThreadUrlInMailingList threadUrl : threadUrls) {
            Assertions.assertEquals("https://mail.openjdk.org/pipermail/valhalla-dev/", threadUrl.getMailingListUrl());
        }
        Assertions.assertTrue(threadUrls.stream().anyMatch(p -> p.getThreadUrl().equals("https://mail.openjdk.org/pipermail/valhalla-dev/2022-September/thread.html")));
        Assertions.assertTrue(threadUrls.stream().anyMatch(p -> p.getThreadUrl().equals("https://mail.openjdk.org/pipermail/valhalla-dev/2021-August/thread.html")));
        Assertions.assertFalse(threadUrls.stream().anyMatch(p -> p.getThreadUrl().equals("https://mail.openjdk.org/pipermail/valhalla-dev/2021-May/thread.html")));
        Assertions.assertTrue(threadUrls.stream().anyMatch(p -> p.getThreadUrl().equals("https://mail.openjdk.org/pipermail/valhalla-dev/2014-July/thread.html")));
    }

    @Test
    public void parseMonthPageTest() throws IOException {
        InputStream contentIs = getClass().getResourceAsStream("/monthPage.html");
        String html = new String(contentIs.readAllBytes(), StandardCharsets.UTF_8);
        List<EmailEntity> emailEntities = Indexer.parseMonthPage(html, Set.of(), "https://mail.openjdk.org/pipermail/amber-dev/2021-December/thread.html");

        Assertions.assertEquals(13, emailEntities.size());

        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007179.html", emailEntities.get(0).getUrl());
        Assertions.assertEquals(0, emailEntities.get(0).getIndent());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/thread.html", emailEntities.get(0).getMonthUrl());
        Assertions.assertNull(emailEntities.get(0).getReplyToUrl());

        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007188.html", emailEntities.get(8).getUrl());
        Assertions.assertEquals(2, emailEntities.get(8).getIndent());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/thread.html", emailEntities.get(8).getMonthUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007186.html", emailEntities.get(8).getReplyToUrl());

        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007181.html", emailEntities.get(2).getUrl());
        Assertions.assertEquals(2, emailEntities.get(2).getIndent());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/thread.html", emailEntities.get(2).getMonthUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007180.html", emailEntities.get(2).getReplyToUrl());

        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007182.html", emailEntities.get(3).getUrl());
        Assertions.assertEquals(3, emailEntities.get(3).getIndent());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/thread.html", emailEntities.get(3).getMonthUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007181.html", emailEntities.get(3).getReplyToUrl());

        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007179.html", emailEntities.get(1).getReplyToUrl());
        Assertions.assertNull(emailEntities.get(4).getReplyToUrl());
        Assertions.assertNull(emailEntities.get(5).getReplyToUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007184.html", emailEntities.get(6).getReplyToUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007186.html", emailEntities.get(7).getReplyToUrl());
        Assertions.assertNull(emailEntities.get(9).getReplyToUrl());
        Assertions.assertNull(emailEntities.get(10).getReplyToUrl());
        Assertions.assertNull(emailEntities.get(11).getReplyToUrl());
        Assertions.assertNull(emailEntities.get(12).getReplyToUrl());

        // now we add some filters
        emailEntities = Indexer.parseMonthPage(html, Set.of("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007181.html"), "https://mail.openjdk.org/pipermail/amber-dev/2021-December/thread.html");

        Assertions.assertEquals(12, emailEntities.size());
        Assertions.assertTrue(emailEntities.stream().map(EmailEntity::getUrl).noneMatch(u -> u.equals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007181.html")));

        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007182.html", emailEntities.get(2).getUrl());
        Assertions.assertEquals(3, emailEntities.get(2).getIndent());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/thread.html", emailEntities.get(2).getMonthUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007181.html", emailEntities.get(2).getReplyToUrl());
    }

    @Test
    public void parseFirstEmailPageTest() throws IOException, ParseException {
        InputStream contentIs = getClass().getResourceAsStream("/firstEmailPage.html");
        String html = new String(contentIs.readAllBytes(), StandardCharsets.UTF_8);

        EmailEntity emailForIndex = new EmailEntity("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007179.html", "https://mail.openjdk.org/pipermail/amber-dev/2021-December/thread.html", 0, null);
        Indexer.parseEmail(html, emailForIndex);

        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007179.html", emailForIndex.getUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/thread.html", emailForIndex.getMonthUrl());
        Assertions.assertEquals(0, emailForIndex.getIndent());
        Assertions.assertNull(emailForIndex.getReplyToUrl());
        Assertions.assertTrue(emailForIndex.getContent().contains("That would help. Such a warning would note where there are"));
        Assertions.assertEquals("[External] : Re: Final variable initialization problem with exhaustive switch", emailForIndex.getEmailTitle());
        Assertions.assertEquals("John Rose", emailForIndex.getEmailAuthor());
        Assertions.assertEquals(1638332537L, emailForIndex.getTimestampInSec());
        Assertions.assertNull(emailForIndex.getPreviousUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/amber-dev/2021-December/007180.html", emailForIndex.getNextUrl());
    }

    @Test
    public void parseMiddleEmailPageTest() throws IOException, ParseException {
        InputStream contentIs = getClass().getResourceAsStream("/middleEmailPage.html");
        String html = new String(contentIs.readAllBytes(), StandardCharsets.UTF_8);

        EmailEntity emailForIndex = new EmailEntity("https://mail.openjdk.org/pipermail/valhalla-dev/2022-February/010005.html", "https://mail.openjdk.org/pipermail/valhalla-dev/2022-February/thread.html", 1, "https://mail.openjdk.org/pipermail/valhalla-dev/2022-February/009984.html");
        Indexer.parseEmail(html, emailForIndex);

        Assertions.assertEquals("https://mail.openjdk.org/pipermail/valhalla-dev/2022-February/010005.html", emailForIndex.getUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/valhalla-dev/2022-February/thread.html", emailForIndex.getMonthUrl());
        Assertions.assertEquals(1, emailForIndex.getIndent());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/valhalla-dev/2022-February/009984.html", emailForIndex.getReplyToUrl());
        Assertions.assertTrue(emailForIndex.getContent().contains("for the review Aggelos"));
        Assertions.assertEquals("[lworld] RFR: 8281026: Allow for compiler.note.cant.instantiate.object.directly to be suppressed via an option", emailForIndex.getEmailTitle());
        Assertions.assertEquals("Srikanth Adayapalam", emailForIndex.getEmailAuthor());
        Assertions.assertEquals(1644409774L, emailForIndex.getTimestampInSec());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/valhalla-dev/2022-February/010002.html", emailForIndex.getPreviousUrl());
        Assertions.assertEquals("https://mail.openjdk.org/pipermail/valhalla-dev/2022-February/010006.html", emailForIndex.getNextUrl());
    }
}