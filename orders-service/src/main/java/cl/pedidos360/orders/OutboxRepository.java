package cl.pedidos360.orders;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OutboxRepository extends JpaRepository<OutboxEntry, UUID> {
    List<OutboxEntry> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
