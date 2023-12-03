package ru.hixon;

import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {

    private static final Logger logger = Logger.getLogger(Database.class.getName());
    private static final int QUERY_TIMEOUT_IN_SECONDS = 10;

    private static final String SQL_UPSERT_EMAIL = "INSERT INTO emails(url, month_url, indent, reply_to_url, content, email_title, email_author, timestamp_in_sec, previous_url, next_url) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(url) DO NOTHING";

    private static final String SQL_UPSERT_MONTH = "INSERT INTO month_indexed(month_url) VALUES(?) ON CONFLICT(month_url) DO NOTHING";

    private final String dbUrl;

    public Database(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Creates tables and indexes, if needed
     */
    public void executeDatabaseMigrations() {
        logger.info("execution database migrations");

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(dbUrl);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(QUERY_TIMEOUT_IN_SECONDS);

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS emails ( " +
                    "url VARCHAR(128) PRIMARY KEY NOT NULL, " +
                    "month_url VARCHAR(128) NOT NULL, " +
                    "indent INTEGER NOT NULL, " +
                    "reply_to_url VARCHAR(128), " +
                    "content TEXT NOT NULL, " +
                    "email_title VARCHAR(512) NOT NULL, " +
                    "email_author VARCHAR(128) NOT NULL, " +
                    "timestamp_in_sec INTEGER NOT NULL, " +
                    "previous_url VARCHAR(128), " +
                    "next_url VARCHAR(128) " +
                    ")");

            statement.executeUpdate("CREATE INDEX IF NOT EXISTS emails_month_url_idx ON emails(month_url)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS month_indexed ( " +
                    "month_url VARCHAR(128) PRIMARY KEY NOT NULL " +
                    ")");
        } catch (Exception e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            logger.log(Level.SEVERE, "Got error, when execute migration", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Db connection close failed", e);
            }
        }
    }

    public Set<String> getIndexedMonthUrls() {
        logger.info("execution getIndexedMonthUrls()");

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(dbUrl);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(QUERY_TIMEOUT_IN_SECONDS);

            ResultSet rs = statement.executeQuery("SELECT month_url FROM month_indexed");
            Set<String> result = new HashSet<>();
            while (rs.next()) {
                result.add(rs.getString("month_url"));
            }
            return result;
        } catch (Exception e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            logger.log(Level.SEVERE, "Got error, when execute getIndexedMonthUrls()", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Db connection close failed", e);
            }
        }
    }

    public Set<String> getIndexedEmailsInGivenMonth(String threadUrl) {
        logger.info("execution getIndexedEmailsInGivenMonth() with url=" + threadUrl);

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(dbUrl);
            PreparedStatement  statement = connection.prepareStatement("SELECT url FROM emails WHERE month_url = ? ");
            statement.setQueryTimeout(QUERY_TIMEOUT_IN_SECONDS);
            statement.setString(1, threadUrl);

            ResultSet rs = statement.executeQuery();
            Set<String> result = new HashSet<>();
            while (rs.next()) {
                result.add(rs.getString("url"));
            }
            return result;
        } catch (Exception e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            logger.log(Level.SEVERE, "Got error, when execute getIndexedMonthUrls()", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Db connection close failed", e);
            }
        }
    }

    public void saveEmailsForGivenMonth(List<EmailEntity> emailsForIndex) {
        if (emailsForIndex.isEmpty()) {
            return;
        }

        logger.info("execution saveEmailsForGivenMonth(), monthUrl=" + emailsForIndex.get(0).getMonthUrl());

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(dbUrl);

            for (EmailEntity forIndex : emailsForIndex) {
                PreparedStatement statement = connection.prepareStatement(SQL_UPSERT_EMAIL);
                statement.setQueryTimeout(QUERY_TIMEOUT_IN_SECONDS);

                statement.setString(1, forIndex.getUrl());
                statement.setString(2, forIndex.getMonthUrl());
                statement.setInt(3, forIndex.getIndent());
                statement.setString(4, forIndex.getReplyToUrl());
                statement.setString(5, forIndex.getContent());
                statement.setString(6, forIndex.getEmailTitle());
                statement.setString(7, forIndex.getEmailAuthor());
                statement.setLong(8, forIndex.getTimestampInSec());
                statement.setString(9, forIndex.getPreviousUrl());
                statement.setString(10, forIndex.getNextUrl());

                statement.executeUpdate();
            }

            PreparedStatement insertMonthStatement = connection.prepareStatement(SQL_UPSERT_MONTH);
            insertMonthStatement.setQueryTimeout(QUERY_TIMEOUT_IN_SECONDS);

            insertMonthStatement.setString(1, emailsForIndex.get(0).getMonthUrl());
            insertMonthStatement.executeUpdate();
        } catch (Exception e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            logger.log(Level.SEVERE, "Got error, when execute saveEmailsForGivenMonth()", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Db connection close failed", e);
            }
        }
    }
}
