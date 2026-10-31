package com.mercor.paymentwebhook.repository;

import com.mercor.paymentwebhook.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
