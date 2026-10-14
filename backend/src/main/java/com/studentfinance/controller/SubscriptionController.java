package com.studentfinance.controller;

import com.studentfinance.dto.SubscriptionRequest;
import com.studentfinance.dto.SubscriptionResponse;
import com.studentfinance.dto.TransactionResponse;
import com.studentfinance.service.SubscriptionService;
import com.studentfinance.service.SubscriptionPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionPaymentService subscriptionPaymentService;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  SubscriptionPaymentService subscriptionPaymentService) {
        this.subscriptionService = subscriptionService;
        this.subscriptionPaymentService = subscriptionPaymentService;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getAll() {
        List<SubscriptionResponse> list = subscriptionService.getAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getById(@PathVariable Long id) {
        SubscriptionResponse response = subscriptionService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse created = subscriptionService.create(request);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> update(@PathVariable Long id, @Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse updated = subscriptionService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<TransactionResponse> markAsPaid(@PathVariable Long id) {
        TransactionResponse created = subscriptionPaymentService.markAsPaid(id);
        return ResponseEntity.status(201).body(created);
    }
}
