package com.studentfinance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;

class BudgetMigrationCompatibilityTest {
    @Test
    void renamesLegacyMonthAndKeepsItsDataAndUniqueness() throws Exception {
        String url = legacyDatabaseUrl();
        createBudgetTable(url, "\"month\" DATE NOT NULL");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO budgets (id, user_id, category_id, \"month\") VALUES (1, 7, 9, DATE '2026-10-01')");
        }

        assertEquals(1, migrateFromBaselineTwo(url));
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT budget_month FROM budgets WHERE id = 1")) {
            assertTrue(result.next());
            assertEquals("2026-10-01", result.getDate(1).toString());
            assertTrue(columnExists(connection, "budget_month"));
            assertTrue(uniqueIndexExists(connection));
        }
    }

    @Test
    void backfillsBudgetMonthWhenBothLegacyAndCurrentColumnsExist() throws Exception {
        String url = legacyDatabaseUrl();
        createBudgetTable(url, "\"month\" DATE NOT NULL, budget_month DATE");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO budgets (id, user_id, category_id, \"month\") VALUES (1, 7, 9, DATE '2026-10-01')");
        }

        assertEquals(1, migrateFromBaselineTwo(url));
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT budget_month FROM budgets WHERE id = 1")) {
            assertTrue(result.next());
            assertEquals("2026-10-01", result.getDate(1).toString());
            assertTrue(uniqueIndexExists(connection));
            assertTrue(columnIsNullable(connection, "month"));
            statement.execute("INSERT INTO budgets (id, user_id, category_id, budget_month) "
                    + "VALUES (2, 7, 9, DATE '2026-11-01')");
        }
    }

    @Test
    void rejectsConflictingNonNullMonthValues() throws Exception {
        String url = legacyDatabaseUrl();
        createBudgetTable(url, "\"month\" DATE, budget_month DATE");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO budgets (id, user_id, category_id, \"month\", budget_month) "
                    + "VALUES (1, 7, 9, DATE '2026-10-01', DATE '2026-11-01')");
        }

        FlywayException exception = assertThrows(FlywayException.class, () -> migrateFromBaselineTwo(url));
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        assertTrue(cause.getMessage().contains("conflicting non-null values in month and budget_month"));
    }

    @Test
    void rejectsUnresolvedNullBudgetMonth() throws Exception {
        String url = legacyDatabaseUrl();
        createBudgetTable(url, "budget_month DATE");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO budgets (id, user_id, category_id, budget_month) VALUES (1, 7, 9, NULL)");
        }

        FlywayException exception = assertThrows(FlywayException.class, () -> migrateFromBaselineTwo(url));
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        assertTrue(cause.getMessage().contains("budget_month is null after legacy month backfill"));
    }

    private String legacyDatabaseUrl() {
        return "jdbc:h2:mem:budget_compat_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    }

    private void createBudgetTable(String url, String monthColumns) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE budgets (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, "
                    + "category_id BIGINT NOT NULL, " + monthColumns + ")");
        }
    }

    private int migrateFromBaselineTwo(String url) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineVersion("2")
                .load();
        flyway.baseline();
        return flyway.migrate().migrationsExecuted;
    }

    private boolean columnExists(Connection connection, String column) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "BUDGETS", null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }

    private boolean uniqueIndexExists(Connection connection) throws Exception {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, "BUDGETS", false, false)) {
            while (indexes.next()) {
                if ("IDX_BUDGET_USER_CATEGORY_MONTH_V3".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
            }
        }
        return false;
    }

    private boolean columnIsNullable(Connection connection, String column) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "BUDGETS", null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"));
                }
            }
        }
        return false;
    }
}
