package com.mercor.paymentwebhook.repository;

import com.mercor.paymentwebhook.entity.Fulfillment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {
    long countByOrder_Id(Long orderId);
    List<Fulfillment> findByOrder_Id(Long orderId);
}
