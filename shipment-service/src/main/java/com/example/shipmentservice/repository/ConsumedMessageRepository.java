package com.example.shipmentservice.repository;

import com.example.shipmentservice.model.ConsumedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsumedMessageRepository extends JpaRepository<ConsumedMessage, UUID> {
}
