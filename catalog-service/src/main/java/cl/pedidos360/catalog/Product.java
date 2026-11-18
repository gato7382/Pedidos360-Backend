package cl.pedidos360.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_products")
public class Product {
    @Id
    private UUID id;
    private String name;
    private String category;
    private String description;
    private BigDecimal price;
    private boolean active;
    private Instant createdAt;
    @Version
    private long version;

    protected Product() {}

    Product(String name, String category, String description, BigDecimal price) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.active = true;
        this.createdAt = Instant.now();
    }

    void update(String name, String category, String description, BigDecimal price) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
    }

    void deactivate() { this.active = false; }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
