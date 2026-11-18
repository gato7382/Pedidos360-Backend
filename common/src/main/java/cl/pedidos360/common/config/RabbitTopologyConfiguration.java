package cl.pedidos360.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitTopologyConfiguration {
    public static final String EVENTS_EXCHANGE = "pedidos360.events";
    public static final String DEAD_EXCHANGE = "pedidos360.dlx";
    public static final String AUDIT_QUEUE = "pedidos360.audit.queue";
    public static final String AUDIT_DLQ = "pedidos360.audit.dlq";
    public static final String ORDER_ROUTING_KEY = "pedido.event";
    public static final String DEAD_ROUTING_KEY = "pedido.dead";

    @Bean
    DirectExchange pedidosEventsExchange() {
        return new DirectExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange pedidosDeadLetterExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    @Bean
    Queue auditQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE)
                .quorum()
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue auditDeadLetterQueue() {
        return QueueBuilder.durable(AUDIT_DLQ).quorum().build();
    }

    @Bean
    Binding auditBinding(@Qualifier("auditQueue") Queue auditQueue,
                         @Qualifier("pedidosEventsExchange") DirectExchange pedidosEventsExchange) {
        return BindingBuilder.bind(auditQueue).to(pedidosEventsExchange).with(ORDER_ROUTING_KEY);
    }

    @Bean
    Binding auditDeadLetterBinding(@Qualifier("auditDeadLetterQueue") Queue auditDeadLetterQueue,
                                   @Qualifier("pedidosDeadLetterExchange") DirectExchange pedidosDeadLetterExchange) {
        return BindingBuilder.bind(auditDeadLetterQueue).to(pedidosDeadLetterExchange).with(DEAD_ROUTING_KEY);
    }
}
