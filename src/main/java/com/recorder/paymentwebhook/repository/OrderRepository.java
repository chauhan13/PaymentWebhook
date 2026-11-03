package com.recorder.paymentwebhook.repository;

import com.recorder.paymentwebhook.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
