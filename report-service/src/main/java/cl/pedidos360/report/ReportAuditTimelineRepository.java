package cl.pedidos360.report;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReportAuditTimelineRepository extends JpaRepository<ReportAuditTimeline, UUID> {
    List<ReportAuditTimeline> findTop200ByOrderByOccurredAtDesc();
}
