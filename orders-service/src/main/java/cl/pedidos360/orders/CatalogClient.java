package cl.pedidos360.orders;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class CatalogClient {
    private final RestClient restClient;

    CatalogClient(RestClient.Builder builder,
                  @Value("${CATALOG_SERVICE_URL:http://localhost:3001}") String catalogUrl) {
        this.restClient = builder.baseUrl(catalogUrl).build();
    }

    ProductView getProduct(UUID id, String bearerToken) {
        try {
            ProductView product = restClient.get()
                    .uri("/api/products/{id}", id)
                    .headers(headers -> headers.setBearerAuth(bearerToken))
                    .retrieve()
                    .body(ProductView.class);
            if (product == null || !product.active()) {
                throw new IllegalArgumentException("El producto no está disponible: " + id);
            }
            return product;
        } catch (RestClientException exception) {
            throw new IllegalArgumentException("No fue posible validar el producto " + id
                    + " en el catálogo.", exception);
        }
    }

    record ProductView(UUID id, String name, String category, BigDecimal price, boolean active) {}
}
