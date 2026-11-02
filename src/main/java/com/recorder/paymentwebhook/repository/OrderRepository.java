package com.recorder.paymentwebhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recorder.paymentwebhook.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
