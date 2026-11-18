package cl.pedidos360.common.events;

import java.util.Set;

public final class PedidoEventValidator {
    private static final Set<String> EVENT_TYPES = Set.of(
            "PEDIDO_CREADO", "PEDIDO_ACEPTADO", "PEDIDO_EN_PREPARACION",
            "PEDIDO_DESPACHADO", "PEDIDO_ENTREGADO", "PEDIDO_CANCELADO");

    private PedidoEventValidator() {}

    public static void validate(PedidoEvent event) {
        if (event == null) throw new IllegalArgumentException("El evento está vacío.");
        if (event.eventId() == null) throw new IllegalArgumentException("eventId es obligatorio.");
        if (event.tipo() == null || !EVENT_TYPES.contains(event.tipo())) throw new IllegalArgumentException("tipo no reconocido.");
        // El DTO anterior a la versión 2 no traía un campo version.
        int version = event.version() == null ? 1 : event.version();
        if (version < 1 || version > 2) throw new IllegalArgumentException("version no soportada.");
        if (event.fecha() == null) throw new IllegalArgumentException("fecha es obligatoria.");
        if (event.pedidoId() == null || event.pedidoId().isBlank()) throw new IllegalArgumentException("pedidoId es obligatorio.");
        if (event.clienteId() == null || event.clienteId().isBlank()) throw new IllegalArgumentException("clienteId es obligatorio.");
        if (event.total() == null || event.total().signum() < 0) throw new IllegalArgumentException("total inválido.");
        if (version == 2 && (event.items() == null || event.items().isEmpty())) {
            throw new IllegalArgumentException("items debe contener al menos un producto.");
        }
    }
}
