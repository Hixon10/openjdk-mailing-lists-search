package ru.hixon;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    @TempDir
    File tempDir;

    @Test
    public void canBuildIndexTest() throws IOException {
        Assertions.assertTrue(tempDir.isDirectory());

        String dbUrl = "jdbc:sqlite:" + tempDir.toPath().resolve("newdb.db");
        dbUrl = dbUrl.replace('\\', '/');

        // create index
        new Main().run(dbUrl, List.of("https://mail.openjdk.org/pipermail/detroit-dev/"));

        // query given db
        Database database = new Database(dbUrl);
        Assertions.assertEquals(Set.of("https://mail.openjdk.org/pipermail/detroit-dev/2018-May/thread.html", "https://mail.openjdk.org/pipermail/detroit-dev/2019-May/thread.html"), database.getIndexedMonthUrls());
        Assertions.assertEquals(Set.of("https://mail.openjdk.org/pipermail/detroit-dev/2018-May/000000.html"), database.getIndexedEmailsInGivenMonth("https://mail.openjdk.org/pipermail/detroit-dev/2018-May/thread.html"));
        Assertions.assertEquals(Set.of("https://mail.openjdk.org/pipermail/detroit-dev/2019-May/000002.html", "https://mail.openjdk.org/pipermail/detroit-dev/2019-May/000001.html", "https://mail.openjdk.org/pipermail/detroit-dev/2019-May/000003.html"), database.getIndexedEmailsInGivenMonth("https://mail.openjdk.org/pipermail/detroit-dev/2019-May/thread.html"));

        // reindex
        new Main().run(dbUrl, List.of("https://mail.openjdk.org/pipermail/detroit-dev/"));

        // should have the same result
        Assertions.assertEquals(Set.of("https://mail.openjdk.org/pipermail/detroit-dev/2018-May/thread.html", "https://mail.openjdk.org/pipermail/detroit-dev/2019-May/thread.html"), database.getIndexedMonthUrls());
        Assertions.assertEquals(Set.of("https://mail.openjdk.org/pipermail/detroit-dev/2018-May/000000.html"), database.getIndexedEmailsInGivenMonth("https://mail.openjdk.org/pipermail/detroit-dev/2018-May/thread.html"));
        Assertions.assertEquals(Set.of("https://mail.openjdk.org/pipermail/detroit-dev/2019-May/000002.html", "https://mail.openjdk.org/pipermail/detroit-dev/2019-May/000001.html", "https://mail.openjdk.org/pipermail/detroit-dev/2019-May/000003.html"), database.getIndexedEmailsInGivenMonth("https://mail.openjdk.org/pipermail/detroit-dev/2019-May/thread.html"));
    }
}
