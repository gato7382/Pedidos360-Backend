package cl.pedidos360.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PedidoEvent(
        UUID eventId,
        String tipo,
        Integer version,
        Instant fecha,
        String pedidoId,
        String clienteId,
        BigDecimal total,
        Actor actor,
        String estadoAnterior,
        String estadoNuevo,
        List<Item> items,
        String canal,
        String comuna,
        String sucursalId) {

    public String estadoEfectivo() {
        return estadoNuevo == null ? tipo.substring("PEDIDO_".length()) : estadoNuevo;
    }

    public record Actor(String usuarioId, String rol) {}

    public record Item(
            String productoId,
            String nombre,
            String categoria,
            int cantidad,
            BigDecimal precioUnitario) {}
}
