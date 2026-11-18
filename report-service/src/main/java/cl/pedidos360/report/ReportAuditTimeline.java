package cl.pedidos360.report;

import cl.pedidos360.common.events.AuditTimelineEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_audit_timeline")
class ReportAuditTimeline {
    @Id
    UUID eventId;
    String orderId;
    String eventType;
    String previousState;
    String newState;
    Instant occurredAt;
    String actorId;

    protected ReportAuditTimeline() {}

    ReportAuditTimeline(AuditTimelineEntry entry) {
        this.eventId = entry.eventId();
        this.orderId = entry.pedidoId();
        this.eventType = entry.tipo();
        this.previousState = entry.estadoAnterior();
        this.newState = entry.estadoNuevo();
        this.occurredAt = entry.fecha();
        this.actorId = entry.actorId();
    }
}
