package cl.pedidos360.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditRepository extends JpaRepository<AuditRecord, UUID> {
    List<AuditRecord> findTop200ByOrderByOccurredAtDesc();
    List<AuditRecord> findByOrderIdOrderByOccurredAtAsc(String orderId);
}

interface RabbitAuditRepository extends JpaRepository<RabbitAuditRecord, UUID> {
    List<RabbitAuditRecord> findTop100ByOrderByReceivedAtDesc();
}
