package com.anurag.campusfix.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (dataSource == null) {
            log.warn("CAMPUSFIX DB INIT: DataSource is null. Skipping database initialization.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String dbProduct = metaData.getDatabaseProductName();
            log.info("CAMPUSFIX DB INIT: Connected to {} database. Current catalog/database: {}", dbProduct, catalog);

            boolean usersTableExists = checkTableExists(conn, "users");
            if (!usersTableExists) {
                log.warn("CAMPUSFIX DB INIT: Table 'users' not found in database '{}'. Executing schema.sql to initialize all tables...", catalog);
                executeSchemaSql(conn);
                boolean verified = checkTableExists(conn, "users");
                if (verified) {
                    log.info("CAMPUSFIX DB INIT: Database tables successfully created from schema.sql!");
                } else {
                    log.error("CAMPUSFIX DB INIT: Table 'users' still not found after executing schema.sql. Please verify database user privileges.");
                }
            } else {
                log.info("CAMPUSFIX DB INIT: Database schema verified. Table 'users' is ready.");
            }
        } catch (Exception ex) {
            log.error("CAMPUSFIX DB INIT: Exception during schema check or initialization: {}", ex.getMessage(), ex);
        }
    }

    private boolean checkTableExists(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM " + tableName + " LIMIT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void executeSchemaSql(Connection conn) {
        try {
            ClassPathResource resource = new ClassPathResource("schema.sql");
            if (!resource.exists()) {
                log.error("CAMPUSFIX DB INIT: schema.sql resource not found on classpath!");
                return;
            }
            String content;
            try (InputStream is = resource.getInputStream()) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            List<String> statements = splitSqlStatements(content);
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            stmt.execute(trimmed);
                        } catch (Exception stmtEx) {
                            log.warn("CAMPUSFIX DB INIT: SQL execution notice: {} (SQL: {})",
                                    stmtEx.getMessage(),
                                    trimmed.substring(0, Math.min(60, trimmed.length())));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("CAMPUSFIX DB INIT: Failed to execute schema.sql: {}", ex.getMessage(), ex);
        }
    }

    public static List<String> splitSqlStatements(String sql) {
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inLineComment = false;
        boolean inBlockComment = false;
        int len = sql.length();

        for (int i = 0; i < len; i++) {
            char c = sql.charAt(i);

            if (inLineComment) {
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                if (c == '*' && i + 1 < len && sql.charAt(i + 1) == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
                inLineComment = true;
                i++;
                continue;
            }

            if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            if (c == ';') {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    list.add(stmt);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String last = current.toString().trim();
        if (!last.isEmpty()) {
            list.add(last);
        }
        return list;
    }
}
