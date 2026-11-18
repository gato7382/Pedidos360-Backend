package cl.pedidos360.report;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
class ReportController {
    private final ReportEventRepository reports;

    ReportController(ReportEventRepository reports) {
        this.reports = reports;
    }

    @GetMapping("/summary")
    Summary summary() {
        List<ReportEvent> events = reports.findAllByOrderByOccurredAtAsc();
        long created = reports.countByEventType("PEDIDO_CREADO");
        long delivered = reports.countByEventType("PEDIDO_ENTREGADO");
        long cancelled = reports.countByEventType("PEDIDO_CANCELADO");
        BigDecimal sales = events.stream()
                .filter(event -> event.eventType.equals("PEDIDO_CREADO"))
                .map(event -> event.total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Instant> createdAt = new HashMap<>();
        Map<String, Instant> deliveredAt = new HashMap<>();
        for (ReportEvent event : events) {
            if (event.eventType.equals("PEDIDO_CREADO")) createdAt.put(event.orderId, event.occurredAt);
            if (event.eventType.equals("PEDIDO_ENTREGADO")) deliveredAt.put(event.orderId, event.occurredAt);
        }
        double avgLeadTime = deliveredAt.entrySet().stream()
                .filter(entry -> createdAt.containsKey(entry.getKey()))
                .mapToLong(entry -> Duration.between(createdAt.get(entry.getKey()), entry.getValue()).toSeconds())
                .average().orElse(0);

        return new Summary(created, delivered, cancelled, sales, avgLeadTime);
    }

    @GetMapping("/events")
    List<ReportEventView> events() {
        return reports.findTop200ByOrderByOccurredAtDesc().stream()
                .map(event -> new ReportEventView(event.orderId, event.eventType, event.state,
                        event.total, event.occurredAt)).toList();
    }

    record Summary(long totalPedidos, long entregados, long cancelados,
                   BigDecimal ventasCreadas, double leadTimePromedioSeg) {}
    record ReportEventView(String pedidoId, String tipo, String estado,
                           BigDecimal total, Instant fecha) {}
}
