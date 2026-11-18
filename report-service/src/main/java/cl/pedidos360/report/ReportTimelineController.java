package cl.pedidos360.report;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/audit-timeline")
class ReportTimelineController {
    private final ReportAuditTimelineRepository timeline;

    ReportTimelineController(ReportAuditTimelineRepository timeline) {
        this.timeline = timeline;
    }

    @GetMapping
    List<TimelineView> timeline() {
        return timeline.findTop200ByOrderByOccurredAtDesc().stream()
                .map(entry -> new TimelineView(entry.eventId, entry.orderId, entry.eventType,
                        entry.previousState, entry.newState, entry.occurredAt, entry.actorId))
                .toList();
    }

    record TimelineView(UUID eventId, String pedidoId, String tipo, String estadoAnterior,
                        String estadoNuevo, Instant fecha, String actorId) {}
}
