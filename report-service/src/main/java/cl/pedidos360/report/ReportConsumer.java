package cl.pedidos360.report;

import cl.pedidos360.common.events.PedidoEvent;
import cl.pedidos360.common.events.PedidoEventValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ReportConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReportConsumer.class);
    private final ReportEventRepository reports;
    private final ObjectMapper mapper;

    ReportConsumer(ReportEventRepository reports, ObjectMapper mapper) {
        this.reports = reports;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "pedidos.eventos", groupId = "${KAFKA_REPORT_GROUP:report-group}")
    @Transactional
    void consume(String payload) {
        try {
            PedidoEvent event = mapper.readValue(payload, PedidoEvent.class);
            PedidoEventValidator.validate(event);
            if (!reports.existsByEventId(event.eventId())) {
                reports.save(new ReportEvent(event.eventId(), event.pedidoId(), event.tipo(),
                        event.estadoEfectivo(), event.clienteId(), event.total(), event.fecha()));
            }
            log.info("Evento {} procesado por report-group para pedido {}", event.eventId(), event.pedidoId());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("El mensaje de Kafka no es un evento JSON válido.", exception);
        }
    }
}
