package cl.pedidos360.orders;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customer_orders")
public class OrderEntity {
    @Id
    private UUID id;
    private String customerId;
    private String state;
    private BigDecimal total;
    private String channel;
    private String commune;
    private String branchId;
    private Instant createdAt;
    private Instant updatedAt;
    @ElementCollection
    @CollectionTable(name = "customer_order_items", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "item_position")
    private List<OrderItemEntity> items = new ArrayList<>();
    @Version
    private long version;

    protected OrderEntity() {}

    OrderEntity(String customerId, String channel, String commune, String branchId,
                List<OrderItemEntity> items, BigDecimal total) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.state = "CREADO";
        this.channel = channel;
        this.commune = commune;
        this.branchId = branchId;
        this.items = new ArrayList<>(items);
        this.total = total;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    String getState() { return state; }
    void setState(String state) { this.state = state; this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getStateValue() { return state; }
    public BigDecimal getTotal() { return total; }
    public String getChannel() { return channel; }
    public String getCommune() { return commune; }
    public String getBranchId() { return branchId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderItemEntity> getItems() { return List.copyOf(items); }
}
