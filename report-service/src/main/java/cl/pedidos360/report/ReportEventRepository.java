package cl.pedidos360.report;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReportEventRepository extends JpaRepository<ReportEvent, UUID> {
    boolean existsByEventId(UUID eventId);
    List<ReportEvent> findAllByOrderByOccurredAtAsc();
    List<ReportEvent> findTop200ByOrderByOccurredAtDesc();
    long countByEventType(String eventType);
}
