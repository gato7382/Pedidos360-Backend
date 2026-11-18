package cl.pedidos360.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findAllByActiveTrueOrderByNameAsc();
    Optional<Product> findByIdAndActiveTrue(UUID id);
}
