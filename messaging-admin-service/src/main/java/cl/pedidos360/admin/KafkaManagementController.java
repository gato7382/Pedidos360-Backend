package cl.pedidos360.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/kafka")
class KafkaManagementController {
    private static final Pattern TOPIC_NAME = Pattern.compile("[a-zA-Z0-9._-]{1,249}");
    private final AdminClient admin;

    KafkaManagementController(AdminClient admin) {
        this.admin = admin;
    }

    @GetMapping("/topics")
    List<TopicView> topics() throws Exception {
        return admin.listTopics().names().get().stream()
                .filter(name -> !name.startsWith("__"))
                .sorted()
                .map(name -> {
                    try { return describeTopic(name); }
                    catch (Exception exception) { throw new IllegalStateException(exception); }
                }).toList();
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    TopicView createTopic(@Valid @RequestBody CreateTopicRequest request) throws Exception {
        validateTopic(request.name());
        if (request.replicas() < 1) throw new IllegalArgumentException("replicas debe ser al menos 1.");
        admin.createTopics(List.of(new NewTopic(request.name(), request.partitions(),
                (short) request.replicas()).configs(configs(request.retentionMs(), request.cleanupPolicy()))))
                .all().get();
        return describeTopic(request.name());
    }

    @PutMapping("/topics/{name}")
    TopicView updateTopic(@PathVariable String name, @Valid @RequestBody UpdateTopicRequest request) throws Exception {
        validateTopic(name);
        TopicDescription current = admin.describeTopics(List.of(name)).allTopicNames().get().get(name);
        int existingPartitions = current.partitions().size();
        if (request.partitions() < existingPartitions) {
            throw new IllegalArgumentException("Kafka permite aumentar particiones, pero no reducirlas.");
        }
        if (request.partitions() > existingPartitions) {
            admin.createPartitions(Map.of(name, NewPartitions.increaseTo(request.partitions()))).all().get();
        }
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        var entries = List.of(
                alter(TopicConfig.RETENTION_MS_CONFIG, Long.toString(request.retentionMs())),
                alter(TopicConfig.CLEANUP_POLICY_CONFIG, request.cleanupPolicy()));
        admin.incrementalAlterConfigs(Map.of(resource, entries)).all().get();
        return describeTopic(name);
    }

    @DeleteMapping("/topics/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTopic(@PathVariable String name) throws Exception {
        validateTopic(name);
        admin.deleteTopics(List.of(name)).all().get();
    }

    @GetMapping("/groups")
    List<GroupView> groups() throws Exception {
        return admin.listConsumerGroups().all().get().stream().map(group -> {
            try {
                var description = admin.describeConsumerGroups(List.of(group.groupId())).all().get().get(group.groupId());
                return new GroupView(group.groupId(), description.state().toString(),
                        description.members().size(), group.isSimpleConsumerGroup());
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }).toList();
    }

    @GetMapping("/groups/{groupId}/offsets")
    List<OffsetView> offsets(
            @PathVariable String groupId) throws Exception {
        return admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get().entrySet().stream()
                .map(entry -> new OffsetView(entry.getKey().topic(), entry.getKey().partition(),
                        entry.getValue().offset(), entry.getValue().metadata()))
                .toList();
    }

    @DeleteMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteGroup(@PathVariable String groupId) throws Exception {
        if (groupId.isBlank()) throw new IllegalArgumentException("groupId es obligatorio.");
        admin.deleteConsumerGroups(List.of(groupId)).all().get();
    }

    private TopicView describeTopic(String name) throws Exception {
        TopicDescription description = admin.describeTopics(List.of(name)).allTopicNames().get().get(name);
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Config config = admin.describeConfigs(List.of(resource)).all().get().get(resource);
        Map<String, String> settings = config.entries().stream()
                .filter(entry -> !entry.isSensitive() && entry.value() != null)
                .collect(java.util.stream.Collectors.toMap(ConfigEntry::name, ConfigEntry::value));
        return new TopicView(name, description.partitions().size(),
                description.partitions().stream().map(partition -> partition.replicas().size())
                        .min(Integer::compareTo).orElse(0), settings);
    }

    private Map<String, String> configs(long retentionMs, String cleanupPolicy) {
        return Map.of(TopicConfig.RETENTION_MS_CONFIG, Long.toString(retentionMs),
                TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy);
    }

    private org.apache.kafka.clients.admin.AlterConfigOp alter(String key, String value) {
        return new org.apache.kafka.clients.admin.AlterConfigOp(
                new ConfigEntry(key, value), org.apache.kafka.clients.admin.AlterConfigOp.OpType.SET);
    }

    private void validateTopic(String name) {
        if (!TOPIC_NAME.matcher(name).matches() || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Nombre de tópico inválido.");
        }
    }

    record CreateTopicRequest(@NotBlank String name, @Min(1) int partitions,
                              @Min(1) int replicas, @Min(1) long retentionMs,
                              @NotBlank String cleanupPolicy) {}
    record UpdateTopicRequest(@Min(1) int partitions, @Min(1) long retentionMs,
                              @NotBlank String cleanupPolicy) {}
    record TopicView(String name, int partitions, int minReplicationFactor, Map<String, String> config) {}
    record GroupView(String groupId, String state, int memberCount, boolean simpleConsumerGroup) {}
    record OffsetView(String topic, int partition, long offset, String metadata) {}
}
