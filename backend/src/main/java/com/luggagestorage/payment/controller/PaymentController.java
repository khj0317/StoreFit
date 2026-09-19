package com.luggagestorage.payment.controller;

import com.luggagestorage.payment.dto.PaymentConfirmRequest;
import com.luggagestorage.payment.dto.PaymentReadyResponse;
import com.luggagestorage.payment.dto.PaymentResponse;
import com.luggagestorage.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/me")
    public ResponseEntity<List<PaymentResponse>> getMyPayments() {
        return ResponseEntity.ok(paymentService.getMyPayments());
    }

    @PostMapping("/{storeId}/ready")
    public ResponseEntity<PaymentReadyResponse> ready(@PathVariable Long storeId) {
        return ResponseEntity.ok(paymentService.ready(storeId));
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirm(@Valid @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirm(request));
    }
}
