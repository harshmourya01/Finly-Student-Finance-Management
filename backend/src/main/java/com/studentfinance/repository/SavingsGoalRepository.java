package com.studentfinance.repository;

import com.studentfinance.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findAllByUser_Id(Long userId);

    @Query("select g from SavingsGoal g where g.targetDate in :dates")
    List<SavingsGoal> findAllWithTargetDateIn(@Param("dates") List<LocalDate> dates);
}
