package cl.pedidos360.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rabbit_audit_events")
class RabbitAuditRecord {
    @Id
    UUID eventId;
    String orderId;
    String eventType;
    Instant receivedAt;

    protected RabbitAuditRecord() {}

    RabbitAuditRecord(UUID eventId, String orderId, String eventType) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.receivedAt = Instant.now();
    }
}
