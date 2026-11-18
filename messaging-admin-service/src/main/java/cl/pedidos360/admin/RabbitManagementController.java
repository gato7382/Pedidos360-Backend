package cl.pedidos360.admin;

import java.util.List;
import java.util.Map;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/admin/rabbit")
class RabbitManagementController {
    private final AmqpAdmin admin;
    private final RabbitManagementApi managementApi;

    RabbitManagementController(AmqpAdmin admin, RabbitManagementApi managementApi) {
        this.admin = admin;
        this.managementApi = managementApi;
    }

    @GetMapping("/queues")
    List<Map<String, Object>> queues() { return managementApi.queues(); }

    @GetMapping("/exchanges")
    List<Map<String, Object>> exchanges() { return managementApi.exchanges(); }

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    void createQueue(@RequestBody QueueRequest request) {
        requireName(request.name());
        admin.declareQueue(new Queue(request.name(), request.durable()));
    }

    @DeleteMapping("/queues/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteQueue(@PathVariable String name) {
        requireName(name);
        admin.deleteQueue(name);
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    void createExchange(@RequestBody ExchangeRequest request) {
        requireName(request.name());
        switch (request.type().toLowerCase()) {
            case "direct" -> admin.declareExchange(new DirectExchange(request.name(), request.durable(), false));
            case "topic" -> admin.declareExchange(new TopicExchange(request.name(), request.durable(), false));
            case "fanout" -> admin.declareExchange(new FanoutExchange(request.name(), request.durable(), false));
            default -> throw new IllegalArgumentException("Tipo de exchange debe ser direct, topic o fanout.");
        }
    }

    @DeleteMapping("/exchanges/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteExchange(@PathVariable String name) {
        requireName(name);
        admin.deleteExchange(name);
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    void createBinding(@RequestBody BindingRequest request) {
        requireName(request.exchange());
        requireName(request.queue());
        admin.declareBinding(new Binding(request.queue(), DestinationType.QUEUE,
                request.exchange(), request.routingKey(), Map.of()));
    }

    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBinding(@RequestBody BindingRequest request) {
        requireName(request.exchange());
        requireName(request.queue());
        admin.removeBinding(new Binding(request.queue(), DestinationType.QUEUE,
                request.exchange(), request.routingKey(), Map.of()));
    }

    private void requireName(String name) {
        if (name == null || !name.matches("[a-zA-Z0-9._-]{1,255}")) {
            throw new IllegalArgumentException("Nombre de recurso inválido.");
        }
    }

    record QueueRequest(String name, boolean durable) {}
    record ExchangeRequest(String name, String type, boolean durable) {}
    record BindingRequest(String exchange, String queue, String routingKey) {}
}

@Component
class RabbitManagementApi {
    private final RestClient client;

    RabbitManagementApi(RestClient.Builder builder,
            @Value("${RABBIT_MANAGEMENT_URL:http://localhost:15672}") String url,
            @Value("${RABBIT_USER:pedidos360}") String user,
            @Value("${RABBIT_PASSWORD:pedidos360_local}") String password) {
        this.client = builder.baseUrl(url).defaultHeaders(headers -> headers.setBasicAuth(user, password)).build();
    }

    List<Map<String, Object>> queues() {
        return client.get().uri("/api/queues").retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    List<Map<String, Object>> exchanges() {
        return client.get().uri("/api/exchanges").retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
