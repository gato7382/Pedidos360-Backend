package cl.pedidos360.orders;

import cl.pedidos360.common.config.RabbitTopologyConfiguration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class EventOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventOutboxPublisher.class);
    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final RabbitTemplate rabbit;

    EventOutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka, RabbitTemplate rabbit) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelayString = "${OUTBOX_POLL_MS:500}")
    @Transactional
    void publishPending() {
        for (OutboxEntry entry : outbox.findTop100ByPublishedFalseOrderByCreatedAtAsc()) {
            try {
                kafka.send("pedidos.eventos", entry.getPartitionKey(), entry.getPayload())
                        .get(10, TimeUnit.SECONDS);
                CorrelationData confirmation = new CorrelationData(entry.getId().toString());
                rabbit.convertAndSend(RabbitTopologyConfiguration.EVENTS_EXCHANGE,
                        entry.getRoutingKey(), entry.getPayload(), confirmation);
                if (!confirmation.getFuture().get(10, TimeUnit.SECONDS).isAck()) {
                    throw new IllegalStateException("RabbitMQ no confirmó la publicación.");
                }
                entry.markPublished();
                log.info("Evento {} publicado en Kafka y RabbitMQ", entry.getId());
            } catch (Exception exception) {
                entry.recordFailure();
                log.error("No se pudo publicar el evento {}. Se reintentará.", entry.getId(), exception);
            }
        }
    }
}
