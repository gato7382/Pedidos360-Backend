package cl.pedidos360.common.events;

import java.time.Instant;
import java.util.UUID;

public record AuditTimelineEntry(
        UUID eventId,
        String pedidoId,
        String tipo,
        String estadoAnterior,
        String estadoNuevo,
        Instant fecha,
        String actorId) {}
