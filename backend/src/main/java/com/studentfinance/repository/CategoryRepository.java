package com.studentfinance.repository;

import com.studentfinance.entity.Category;
import com.studentfinance.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByType(TransactionType type);
    Optional<Category> findByNameAndType(String name, TransactionType type);
}
