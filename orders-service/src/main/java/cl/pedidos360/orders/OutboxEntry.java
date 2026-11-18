package cl.pedidos360.orders;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pedido_outbox")
public class OutboxEntry {
    @Id
    private UUID id;
    private String partitionKey;
    private String routingKey;
    private Instant createdAt;
    private boolean published;
    private int attempts;
    @jakarta.persistence.Column(nullable = false, columnDefinition = "text")
    private String payload;

    protected OutboxEntry() {}

    OutboxEntry(UUID id, String partitionKey, String routingKey, String payload) {
        this.id = id;
        this.partitionKey = partitionKey;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.published = false;
    }

    void markPublished() { this.published = true; }
    void recordFailure() { this.attempts++; }

    public UUID getId() { return id; }
    public String getPartitionKey() { return partitionKey; }
    public String getRoutingKey() { return routingKey; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isPublished() { return published; }
    public int getAttempts() { return attempts; }
    public String getPayload() { return payload; }
}
