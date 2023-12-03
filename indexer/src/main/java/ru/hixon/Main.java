package ru.hixon;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private static final List<String> mailingListArchives = List.of(
            "https://mail.openjdk.org/pipermail/valhalla-dev/",
            "https://mail.openjdk.org/pipermail/amber-dev/",
            "https://mail.openjdk.org/pipermail/panama-dev/",
            "https://mail.openjdk.org/pipermail/loom-dev/",
            "https://mail.openjdk.org/pipermail/lilliput-dev/",
            "https://mail.openjdk.org/pipermail/leyden-dev/",
            "https://mail.openjdk.org/pipermail/jdk-dev/",
            "https://mail.openjdk.org/pipermail/graal-dev/",
            "https://mail.openjdk.org/pipermail/announce/"
    );

    public static void main(String[] args) throws Exception {
        final String dbUrl;
        if (args.length > 0) {
            dbUrl = args[0];
        } else {
            dbUrl = "jdbc:sqlite:../../../docs/newdb.db";
        }

        new Main().run(dbUrl, mailingListArchives);
    }

    public void run(String dbUrl, List<String> mailingList) throws IOException {
        LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));

        logger.info("Using dbUrl=" + dbUrl);

        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            Database database = new Database(dbUrl);
            database.executeDatabaseMigrations();

            Indexer indexer = new Indexer(database, httpClient, mailingList);
            indexer.index();
        } catch (Throwable th) {
            logger.log(Level.SEVERE, "Got unhandled exception", th);
        }
    }
}