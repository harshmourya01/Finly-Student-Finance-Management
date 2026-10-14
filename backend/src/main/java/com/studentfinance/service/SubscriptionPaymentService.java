package com.studentfinance.service;

import com.studentfinance.dto.TransactionRequest;
import com.studentfinance.dto.TransactionResponse;
import com.studentfinance.entity.Category;
import com.studentfinance.entity.PaymentMethod;
import com.studentfinance.entity.Subscription;
import com.studentfinance.entity.SubscriptionStatus;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.repository.CategoryRepository;
import com.studentfinance.repository.SubscriptionRepository;
import com.studentfinance.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/** Creates a transaction for an authenticated user's subscription under a row lock. */
@Service
public class SubscriptionPaymentService {
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;
    private final SubscriptionService subscriptionService;
    private final TransactionService transactionService;

    public SubscriptionPaymentService(SubscriptionRepository subscriptionRepository,
                                     TransactionRepository transactionRepository,
                                     CategoryRepository categoryRepository,
                                     CurrentUserService currentUserService,
                                     SubscriptionService subscriptionService,
                                     TransactionService transactionService) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
        this.subscriptionService = subscriptionService;
        this.transactionService = transactionService;
    }

    @Transactional
    public TransactionResponse markAsPaid(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        Long userId = currentUserService.getCurrentUser().getId();
        if (!subscription.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found");
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only active subscriptions can be marked as paid");
        }

        var existingTransactions = transactionRepository.findAllByUser_Id(userId);
        if (subscriptionService.hasPaidCurrentCycle(subscription, existingTransactions, LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A payment is already recorded for this subscription's current billing cycle");
        }

        Category category = categoryRepository.findByType(TransactionType.EXPENSE).stream()
                .filter(item -> item.getName().equalsIgnoreCase(subscription.getCategory()))
                .findFirst()
                .or(() -> categoryRepository.findByNameAndType("Other", TransactionType.EXPENSE))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No matching expense category is available for this subscription"));

        TransactionRequest request = new TransactionRequest(
                subscription.getAmount(), LocalDate.now(), subscription.getName(), TransactionType.EXPENSE,
                PaymentMethod.OTHER, category.getId(), SubscriptionService.paymentNoteMarker(subscription));
        return transactionService.create(request);
    }
}
