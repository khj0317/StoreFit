package com.luggagestorage.payment.repository;

import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStore(Store store);

    Optional<Payment> findByOrderId(String orderId);
}
