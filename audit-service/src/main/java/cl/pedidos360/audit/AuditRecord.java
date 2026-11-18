package cl.pedidos360.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
class AuditRecord {
    @Id
    UUID eventId;
    String orderId;
    String customerId;
    String eventType;
    String previousState;
    String newState;
    Instant occurredAt;
    BigDecimal total;
    @Column(nullable = false, columnDefinition = "text")
    String payload;

    protected AuditRecord() {}

    AuditRecord(UUID eventId, String orderId, String customerId, String eventType,
                String previousState, String newState, Instant occurredAt,
                BigDecimal total, String payload) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.eventType = eventType;
        this.previousState = previousState;
        this.newState = newState;
        this.occurredAt = occurredAt;
        this.total = total;
        this.payload = payload;
    }
}
