package cl.pedidos360.admin;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KafkaAdminConfiguration {
    @Bean(destroyMethod = "close")
    AdminClient kafkaAdminClient(KafkaProperties properties, SslBundles sslBundles) {
        Map<String, Object> config = new HashMap<>(properties.buildAdminProperties(sslBundles));
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(AdminClientConfig.CLIENT_ID_CONFIG, "pedidos360-admin");
        return AdminClient.create(config);
    }
}
