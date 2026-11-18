package cl.pedidos360.audit;

import cl.pedidos360.common.config.RabbitTopologyConfiguration;
import cl.pedidos360.common.events.AuditTimelineEntry;
import cl.pedidos360.common.events.PedidoEvent;
import cl.pedidos360.common.events.PedidoEventValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class AuditConsumers {
    private static final Logger log = LoggerFactory.getLogger(AuditConsumers.class);
    private final AuditRepository audit;
    private final RabbitAuditRepository rabbitAudit;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafka;

    AuditConsumers(AuditRepository audit, RabbitAuditRepository rabbitAudit,
                   ObjectMapper mapper, KafkaTemplate<String, String> kafka) {
        this.audit = audit;
        this.rabbitAudit = rabbitAudit;
        this.mapper = mapper;
        this.kafka = kafka;
    }

    @KafkaListener(topics = "pedidos.eventos", groupId = "${KAFKA_AUDIT_GROUP:audit-group}")
    @Transactional(rollbackFor = Exception.class)
    void consumeKafka(String payload) throws Exception {
        PedidoEvent event = readAndValidate(payload);
        if (audit.existsById(event.eventId())) {
            log.info("Evento repetido {} omitido en la tabla de auditoría", event.eventId());
            return;
        }
        audit.save(new AuditRecord(event.eventId(), event.pedidoId(), event.clienteId(),
                event.tipo(), event.estadoAnterior(), event.estadoEfectivo(), event.fecha(),
                event.total(), payload));
        AuditTimelineEntry timeline = new AuditTimelineEntry(event.eventId(), event.pedidoId(),
                event.tipo(), event.estadoAnterior(), event.estadoEfectivo(), event.fecha(),
                event.actor() == null ? null : event.actor().usuarioId());
        kafka.send("audit.timeline", event.pedidoId(), mapper.writeValueAsString(timeline))
                .get(10, TimeUnit.SECONDS);
        log.info("Auditoría Kafka guardada para pedido {}", event.pedidoId());
    }

    @RabbitListener(queues = RabbitTopologyConfiguration.AUDIT_QUEUE)
    @Transactional
    void consumeRabbit(String payload) {
        PedidoEvent event = readAndValidate(payload);
        if (!rabbitAudit.existsById(event.eventId())) {
            rabbitAudit.save(new RabbitAuditRecord(event.eventId(), event.pedidoId(), event.tipo()));
        }
        log.info("Evento RabbitMQ registrado para pedido {}", event.pedidoId());
    }

    private PedidoEvent readAndValidate(String payload) {
        try {
            PedidoEvent event = mapper.readValue(payload, PedidoEvent.class);
            PedidoEventValidator.validate(event);
            return event;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("El mensaje no contiene un evento JSON válido.", exception);
        }
    }

    @RabbitListener(queues = RabbitTopologyConfiguration.AUDIT_DLQ)
    void inspectRabbitDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("Mensaje RabbitMQ enviado a {} (ID={}, muertes={})",
                RabbitTopologyConfiguration.AUDIT_DLQ, message.getMessageProperties().getMessageId(),
                message.getMessageProperties().getXDeathHeader());
    }
}
