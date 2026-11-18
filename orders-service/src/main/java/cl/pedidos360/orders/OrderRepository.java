package cl.pedidos360.orders;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findAllByOrderByCreatedAtDesc();
    List<OrderEntity> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);
}
