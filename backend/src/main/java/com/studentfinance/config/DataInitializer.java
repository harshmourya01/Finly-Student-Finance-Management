package com.studentfinance.config;

import com.studentfinance.entity.Category;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            List<Category> defaultCategories = List.of(
                    Category.builder().name("Food").type(TransactionType.EXPENSE).build(),
                    Category.builder().name("Transportation").type(TransactionType.EXPENSE).build(),
                    Category.builder().name("Shopping").type(TransactionType.EXPENSE).build(),
                    Category.builder().name("Bills").type(TransactionType.EXPENSE).build(),
                    Category.builder().name("Entertainment").type(TransactionType.EXPENSE).build(),
                    Category.builder().name("Education").type(TransactionType.EXPENSE).build(),
                    Category.builder().name("Healthcare").type(TransactionType.EXPENSE).build(),
                    Category.builder().name("Other").type(TransactionType.EXPENSE).build(),
                    Category.builder().name("Salary").type(TransactionType.INCOME).build(),
                    Category.builder().name("Freelance").type(TransactionType.INCOME).build(),
                    Category.builder().name("Other").type(TransactionType.INCOME).build()
            );
            categoryRepository.saveAll(defaultCategories);
        }
    }
}
