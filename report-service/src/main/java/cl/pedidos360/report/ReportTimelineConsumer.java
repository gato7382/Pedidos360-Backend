package cl.pedidos360.report;

import cl.pedidos360.common.events.AuditTimelineEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ReportTimelineConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReportTimelineConsumer.class);
    private final ReportAuditTimelineRepository timeline;
    private final ObjectMapper mapper;

    ReportTimelineConsumer(ReportAuditTimelineRepository timeline, ObjectMapper mapper) {
        this.timeline = timeline;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "audit.timeline", groupId = "${KAFKA_TIMELINE_GROUP:audit-timeline-group}")
    @Transactional
    void consume(String payload) {
        try {
            AuditTimelineEntry entry = mapper.readValue(payload, AuditTimelineEntry.class);
            if (entry.eventId() == null || entry.pedidoId() == null || entry.fecha() == null) {
                throw new IllegalArgumentException("Faltan campos requeridos en audit.timeline.");
            }
            if (!timeline.existsById(entry.eventId())) {
                timeline.save(new ReportAuditTimeline(entry));
            }
            log.info("audit.timeline procesado para pedido {}", entry.pedidoId());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("El registro de audit.timeline no es JSON válido.", exception);
        }
    }
}
