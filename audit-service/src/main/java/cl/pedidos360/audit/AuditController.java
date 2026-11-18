package cl.pedidos360.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
class AuditController {
    private final AuditRepository audit;
    private final RabbitAuditRepository rabbitAudit;

    AuditController(AuditRepository audit, RabbitAuditRepository rabbitAudit) {
        this.audit = audit;
        this.rabbitAudit = rabbitAudit;
    }

    @GetMapping("/events")
    List<AuditView> list(@RequestParam(required = false) String pedidoId) {
        List<AuditRecord> rows = pedidoId == null
                ? audit.findTop200ByOrderByOccurredAtDesc()
                : audit.findByOrderIdOrderByOccurredAtAsc(pedidoId);
        return rows.stream().map(AuditView::from).toList();
    }

    @GetMapping("/rabbit-events")
    List<RabbitAuditView> listRabbitEvents() {
        return rabbitAudit.findTop100ByOrderByReceivedAtDesc().stream()
                .map(row -> new RabbitAuditView(row.eventId, row.orderId, row.eventType, row.receivedAt)).toList();
    }

    record AuditView(UUID eventId, String pedidoId, String clienteId, String tipo,
                     String estadoAnterior, String estadoNuevo, Instant fecha) {
        static AuditView from(AuditRecord row) {
            return new AuditView(row.eventId, row.orderId, row.customerId, row.eventType,
                    row.previousState, row.newState, row.occurredAt);
        }
    }

    record RabbitAuditView(UUID eventId, String pedidoId, String tipo, Instant recibidaEn) {}
}
