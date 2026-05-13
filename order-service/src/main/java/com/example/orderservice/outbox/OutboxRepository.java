package com.example.orderservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {
}
