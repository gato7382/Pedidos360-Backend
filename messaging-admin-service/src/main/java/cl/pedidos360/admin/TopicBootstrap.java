package cl.pedidos360.admin;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TopicBootstrap {
    @Bean
    ApplicationRunner createRequiredTopics(AdminClient kafka,
            @Value("${KAFKA_REPLICATION_FACTOR:3}") short replicationFactor) {
        return args -> {
            List<NewTopic> required = List.of(
                    topic("pedidos.eventos", 3, replicationFactor, Map.of(
                            "retention.ms", "604800000", "cleanup.policy", "delete")),
                    topic("pedidos.eventos.audit.dlt", 3, replicationFactor, Map.of(
                            "retention.ms", "1209600000", "cleanup.policy", "delete")),
                    topic("pedidos.eventos.report.dlt", 3, replicationFactor, Map.of(
                            "retention.ms", "1209600000", "cleanup.policy", "delete")),
                    topic("audit.timeline", 3, replicationFactor, Map.of(
                            "retention.ms", "2592000000", "cleanup.policy", "delete")));
            for (NewTopic topic : required) {
                try {
                    kafka.createTopics(List.of(topic)).all().get();
                } catch (java.util.concurrent.ExecutionException exception) {
                    if (!(exception.getCause() instanceof TopicExistsException)) throw exception;
                }
            }
        };
    }

    private NewTopic topic(String name, int partitions, short replicas, Map<String, String> configs) {
        return new NewTopic(name, partitions, replicas).configs(configs);
    }
}
