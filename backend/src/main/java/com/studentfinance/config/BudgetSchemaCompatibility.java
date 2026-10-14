package com.studentfinance.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps databases upgraded from the old budgets.month column writable. */
@Component
@Order(1)
public class BudgetSchemaCompatibility implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public BudgetSchemaCompatibility(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer matchingColumns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = current_schema() AND table_name = 'budgets' " +
                        "AND column_name IN ('month', 'budget_month')",
                Integer.class);
        if (matchingColumns != null && matchingColumns == 2) {
            // The old column may contain legacy data, so keep it and only remove its obsolete constraint.
            jdbcTemplate.execute("ALTER TABLE budgets ALTER COLUMN month DROP NOT NULL");
        }
    }
}
