package com.studentfinance.repository;

import com.studentfinance.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findAllByUser_Id(Long userId);
    List<Budget> findAllByUser_IdAndMonth(Long userId, LocalDate month);
    Optional<Budget> findByUser_IdAndCategory_IdAndMonth(Long userId, Long categoryId, LocalDate month);
}
