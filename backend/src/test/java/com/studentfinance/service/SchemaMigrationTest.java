package com.studentfinance.service;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class SchemaMigrationTest {
    @Test
    void freshDatabaseMigrationsCreateAdditiveNotificationAndIdempotencySchema() throws Exception {
        String url = "jdbc:h2:mem:migration_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load();

        assertEquals(3, flyway.migrate().migrationsExecuted);
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            var metadata = connection.getMetaData();
            assertTrue(tableExists(metadata, "notifications"));
            assertTrue(tableExists(metadata, "financial_idempotency_requests"));
            assertTrue(tableExists(metadata, "transactions"));
            assertEquals(Set.of("ID", "CREATED_AT", "UPDATED_AT", "AMOUNT", "BUDGET_MONTH", "CATEGORY_ID", "USER_ID"),
                    columnNames(metadata, "budgets"));
            assertTrue(uniqueIndexExists(metadata, "budgets", Set.of("USER_ID", "CATEGORY_ID", "BUDGET_MONTH")));
            assertTrue(foreignKeyExists(metadata, "budgets", "USER_ID", "APP_USERS"));
            assertTrue(foreignKeyExists(metadata, "budgets", "CATEGORY_ID", "CATEGORIES"));
            assertTrue(uniqueIndexExists(metadata, "notifications", Set.of("USER_ID", "DEDUPLICATION_KEY")));
            assertTrue(uniqueIndexExists(metadata, "financial_idempotency_requests",
                    Set.of("USER_ID", "OPERATION_TYPE", "IDEMPOTENCY_KEY")));
            assertTrue(indexExists(metadata, "notifications", "idx_notification_user_created"));
            assertTrue(indexExists(metadata, "notifications", "idx_notification_user_read"));
        }
    }

    private boolean tableExists(java.sql.DatabaseMetaData metadata, String name) throws Exception {
        try (var tables = metadata.getTables(null, null, name.toUpperCase(), new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    private boolean indexExists(java.sql.DatabaseMetaData metadata, String table, String index) throws Exception {
        try (var indexes = metadata.getIndexInfo(null, null, table.toUpperCase(), false, false)) {
            while (indexes.next()) {
                if (index.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
            }
        }
        return false;
    }

    private Set<String> columnNames(java.sql.DatabaseMetaData metadata, String table) throws Exception {
        Set<String> names = new HashSet<>();
        try (var columns = metadata.getColumns(null, null, table.toUpperCase(), null)) {
            while (columns.next()) names.add(columns.getString("COLUMN_NAME").toUpperCase());
        }
        return names;
    }

    private boolean foreignKeyExists(java.sql.DatabaseMetaData metadata, String table, String column,
                                     String referencedTable) throws Exception {
        try (var keys = metadata.getImportedKeys(null, null, table.toUpperCase())) {
            while (keys.next()) {
                if (column.equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))
                        && referencedTable.equalsIgnoreCase(keys.getString("PKTABLE_NAME"))) return true;
            }
        }
        return false;
    }

    private boolean uniqueIndexExists(java.sql.DatabaseMetaData metadata, String table, Set<String> columns)
            throws Exception {
        Map<String, Set<String>> uniqueIndexes = new HashMap<>();
        try (var indexes = metadata.getIndexInfo(null, null, table.toUpperCase(), false, false)) {
            while (indexes.next()) {
                if (!indexes.getBoolean("NON_UNIQUE")) {
                    uniqueIndexes.computeIfAbsent(indexes.getString("INDEX_NAME"), ignored -> new HashSet<>())
                            .add(indexes.getString("COLUMN_NAME").toUpperCase());
                }
            }
        }
        return uniqueIndexes.values().stream().anyMatch(columns::equals);
    }
}
