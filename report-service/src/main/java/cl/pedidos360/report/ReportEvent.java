package cl.pedidos360.report;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_events")
class ReportEvent {
    @Id
    UUID eventId;
    String orderId;
    String eventType;
    String state;
    String customerId;
    BigDecimal total;
    Instant occurredAt;

    protected ReportEvent() {}

    ReportEvent(UUID eventId, String orderId, String eventType, String state,
                String customerId, BigDecimal total, Instant occurredAt) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.state = state;
        this.customerId = customerId;
        this.total = total;
        this.occurredAt = occurredAt;
    }
}
