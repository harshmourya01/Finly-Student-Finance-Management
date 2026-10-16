package com.studentfinance.repository;

import com.studentfinance.entity.FinancialIdempotencyRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialIdempotencyRequestRepository extends JpaRepository<FinancialIdempotencyRequest, Long> {
    Optional<FinancialIdempotencyRequest> findByUser_IdAndOperationTypeAndIdempotencyKey(
            Long userId, String operationType, String idempotencyKey);
    List<FinancialIdempotencyRequest> findAllByUser_Id(Long userId);
}
