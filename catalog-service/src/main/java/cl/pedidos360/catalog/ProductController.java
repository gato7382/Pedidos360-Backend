package cl.pedidos360.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
class ProductController {
    private final ProductRepository products;

    ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    List<ProductView> list() {
        return products.findAllByActiveTrueOrderByNameAsc().stream().map(ProductView::from).toList();
    }

    @GetMapping("/{id}")
    ProductView get(@PathVariable UUID id) {
        return ProductView.from(findActive(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    ProductView create(@Valid @RequestBody ProductRequest request) {
        Product saved = products.save(new Product(request.name(), request.category(),
                request.description(), request.price()));
        return ProductView.from(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    ProductView update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        Product product = findActive(id);
        product.update(request.name(), request.category(), request.description(), request.price());
        return ProductView.from(products.save(product));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void delete(@PathVariable UUID id) {
        findActive(id).deactivate();
    }

    private Product findActive(UUID id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
    }

    record ProductRequest(
            @NotBlank String name,
            @NotBlank String category,
            String description,
            @NotNull @DecimalMin("0.01") BigDecimal price) {}

    record ProductView(
            UUID id, String name, String category, String description,
            BigDecimal price, Instant createdAt, boolean active) {
        static ProductView from(Product product) {
            return new ProductView(product.getId(), product.getName(), product.getCategory(),
                    product.getDescription(), product.getPrice(), product.getCreatedAt(), product.isActive());
        }
    }
}
