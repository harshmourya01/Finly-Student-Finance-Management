package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Normalizes the legacy budgets.month column before Hibernate validates the schema. */
public class V3__budget_month_compatibility extends BaseJavaMigration {

    private static final String TABLE = "budgets";
    private static final String LEGACY_MONTH = "month";
    private static final String CURRENT_MONTH = "budget_month";
    private static final String UNIQUE_INDEX = "idx_budget_user_category_month_v3";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean hasLegacyMonth = columnExists(connection, LEGACY_MONTH);
        boolean hasBudgetMonth = columnExists(connection, CURRENT_MONTH);

        if (hasLegacyMonth && !hasBudgetMonth) {
            execute(connection, "ALTER TABLE budgets RENAME COLUMN \"month\" TO budget_month");
        } else if (!hasBudgetMonth) {
            execute(connection, "ALTER TABLE budgets ADD COLUMN budget_month DATE");
        } else if (hasLegacyMonth) {
            if (hasConflictingBudgetMonths(connection)) {
                throw new SQLException("Cannot migrate budgets: conflicting non-null values in month and budget_month");
            }
            execute(connection, "UPDATE budgets SET budget_month = \"month\" WHERE budget_month IS NULL");
            execute(connection, "ALTER TABLE budgets ALTER COLUMN \"month\" DROP NOT NULL");
        }

        if (hasNullBudgetMonths(connection)) {
            throw new SQLException("Cannot migrate budgets: budget_month is null after legacy month backfill");
        }
        execute(connection, "ALTER TABLE budgets ALTER COLUMN budget_month SET NOT NULL");

        if (!uniqueBudgetMonthIndexExists(connection)) {
            execute(connection, "CREATE UNIQUE INDEX " + UNIQUE_INDEX
                    + " ON budgets (user_id, category_id, budget_month)");
        }
    }

    private boolean columnExists(Connection connection, String column) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, TABLE, null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
        }
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, TABLE.toUpperCase(Locale.ROOT), null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }

    private boolean hasNullBudgetMonths(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM budgets WHERE budget_month IS NULL")) {
            result.next();
            return result.getLong(1) > 0;
        }
    }

    private boolean hasConflictingBudgetMonths(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM budgets WHERE \"month\" IS NOT NULL "
                + "AND budget_month IS NOT NULL AND \"month\" <> budget_month";
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1) > 0;
        }
    }

    private boolean uniqueBudgetMonthIndexExists(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        for (String table : new String[] {TABLE, TABLE.toUpperCase(Locale.ROOT)}) {
            Set<String> columns = new HashSet<>();
            String indexName = null;
            try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, table, false, false)) {
                while (indexes.next()) {
                    if (indexes.getBoolean("NON_UNIQUE")) continue;
                    String currentName = indexes.getString("INDEX_NAME");
                    if (indexName != null && !indexName.equalsIgnoreCase(currentName)) {
                        if (isBudgetMonthUnique(columns)) return true;
                        columns.clear();
                    }
                    indexName = currentName;
                    String column = indexes.getString("COLUMN_NAME");
                    if (column != null) columns.add(column.toLowerCase(Locale.ROOT));
                }
            }
            if (isBudgetMonthUnique(columns)) return true;
        }
        return false;
    }

    private boolean isBudgetMonthUnique(Set<String> columns) {
        return columns.equals(Set.of("user_id", "category_id", "budget_month"));
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
