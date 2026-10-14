package com.studentfinance.service;

import com.studentfinance.dto.SubscriptionRequest;
import com.studentfinance.dto.SubscriptionResponse;
import com.studentfinance.entity.Subscription;
import com.studentfinance.entity.SubscriptionFrequency;
import com.studentfinance.entity.SubscriptionStatus;
import com.studentfinance.entity.Transaction;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service layer for managing subscriptions.
 *
 * Ownership rules: a subscription belongs to a user and can only be accessed or modified by that user.
 * The user is obtained from {@link CurrentUserService} which extracts the authenticated user from the security context.
 */
@Service
public class SubscriptionService {

    private static final Pattern PAYMENT_MARKER_PATTERN = Pattern.compile("\\[FINLY_SUBSCRIPTION_PAYMENT:\\d+:\\d{4}-\\d{2}-\\d{2}]" );

    private final SubscriptionRepository subscriptionRepository;
    private final CurrentUserService currentUserService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, CurrentUserService currentUserService) {
        this.subscriptionRepository = subscriptionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getAll() {
        var user = currentUserService.getCurrentUser();
        return subscriptionRepository.findAllByUser_Id(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getById(Long id) {
        var subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        verifyOwnership(subscription);
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        var user = currentUserService.getCurrentUser();
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setName(request.name());
        subscription.setCategory(request.category());
        subscription.setAmount(request.amount());
        subscription.setFrequency(request.frequency());
        subscription.setNextRenewalDate(request.renewal());
        subscription.setStatus(request.status());
        Subscription saved = subscriptionRepository.save(subscription);
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionRequest request) {
        var subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        verifyOwnership(subscription);
        subscription.setName(request.name());
        subscription.setCategory(request.category());
        subscription.setAmount(request.amount());
        subscription.setFrequency(request.frequency());
        subscription.setNextRenewalDate(request.renewal());
        subscription.setStatus(request.status());
        Subscription saved = subscriptionRepository.save(subscription);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        var subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        verifyOwnership(subscription);
        subscriptionRepository.delete(subscription);
    }

    /**
     * Calculates the total monthly cost for the authenticated user.
     * Only ACTIVE subscriptions are considered.
     * YEARLY subscriptions are divided by 12 (rounded half up).
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateMonthlyCost() {
        var user = currentUserService.getCurrentUser();
        List<Subscription> subs = subscriptionRepository.findAllByUser_Id(user.getId())
                .stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .collect(Collectors.toList());
        BigDecimal total = BigDecimal.ZERO;
        for (Subscription s : subs) {
            total = total.add(monthlyAmount(s));
        }
        return total;
    }

    /**
     * Calculates upcoming ACTIVE subscription commitments not already recorded as an expense.
     * A payment is matched to the current renewal cycle by subscription name and full payment amount.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateUnpaidMonthlyCommitments(Long userId, List<Transaction> userTransactions) {
        LocalDate today = LocalDate.now();
        BigDecimal total = BigDecimal.ZERO;
        for (Subscription subscription : subscriptionRepository.findAllByUser_Id(userId)) {
            if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                    || hasPaidCurrentCycle(subscription, userTransactions, today)) {
                continue;
            }
            total = total.add(monthlyAmount(subscription));
        }
        return total;
    }

    private BigDecimal monthlyAmount(Subscription subscription) {
        if (subscription.getFrequency() == SubscriptionFrequency.MONTHLY) {
            return subscription.getAmount();
        }
        return subscription.getAmount().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    public static String paymentNoteMarker(Subscription subscription) {
        return "[FINLY_SUBSCRIPTION_PAYMENT:" + subscription.getId() + ":" + subscription.getNextRenewalDate() + "]";
    }

    public static String preservePaymentMarkers(String previousNotes, String requestedNotes) {
        String result = requestedNotes == null ? "" : requestedNotes.trim();
        if (previousNotes == null) return result.isEmpty() ? null : result;
        Matcher matcher = PAYMENT_MARKER_PATTERN.matcher(previousNotes);
        while (matcher.find()) {
            if (!result.contains(matcher.group())) result = result.isEmpty() ? matcher.group() : result + " " + matcher.group();
        }
        return result.isEmpty() ? null : result;
    }

    public static String userNotes(String notes) {
        if (notes == null) return null;
        String visible = PAYMENT_MARKER_PATTERN.matcher(notes).replaceAll("").trim();
        return visible.isEmpty() ? null : visible;
    }

    public boolean hasPaidCurrentCycle(Subscription subscription, List<Transaction> transactions, LocalDate today) {
        String marker = paymentNoteMarker(subscription);
        if (transactions.stream().anyMatch(transaction -> transaction.getNotes() != null
                && transaction.getNotes().contains(marker))) {
            return true;
        }
        LocalDate cycleStart = subscription.getFrequency() == SubscriptionFrequency.MONTHLY
                ? subscription.getNextRenewalDate().minusMonths(1)
                : subscription.getNextRenewalDate().minusYears(1);
        return transactions.stream().anyMatch(transaction ->
                transaction.getType() == TransactionType.EXPENSE
                        && transaction.getAmount().compareTo(subscription.getAmount()) == 0
                        && transaction.getDescription() != null
                        && transaction.getDescription().trim().equalsIgnoreCase(subscription.getName().trim())
                        && !transaction.getTransactionDate().isBefore(cycleStart)
                        && !transaction.getTransactionDate().isAfter(today));
    }

    private void verifyOwnership(Subscription subscription) {
        var currentUserId = currentUserService.getCurrentUser().getId();
        if (!subscription.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found");
        }
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(
                s.getId(),
                s.getName(),
                s.getCategory(),
                s.getAmount(),
                s.getFrequency(),
                s.getNextRenewalDate(),
                s.getStatus()
        );
    }
}
