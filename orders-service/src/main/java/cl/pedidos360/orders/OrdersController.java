package cl.pedidos360.orders;

import cl.pedidos360.common.events.PedidoEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
class OrdersController {
    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final CatalogClient catalog;
    private final ObjectMapper objectMapper;

    OrdersController(OrderRepository orders, OutboxRepository outbox,
                     CatalogClient catalog, ObjectMapper objectMapper) {
        this.orders = orders;
        this.outbox = outbox;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<OrderView> list(@RequestParam(required = false) String customerId) {
        List<OrderEntity> result = customerId == null || customerId.isBlank()
                ? orders.findAllByOrderByCreatedAtDesc()
                : orders.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        return result.stream().map(OrderView::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    OrderView get(@PathVariable UUID id) {
        return OrderView.from(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    OrderView create(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal Jwt jwt) {
        List<OrderItemEntity> items = request.items().stream().map(item -> {
            CatalogClient.ProductView product = catalog.getProduct(item.productId(), jwt.getTokenValue());
            return new OrderItemEntity(product.id().toString(), product.name(), product.category(),
                    item.quantity(), product.price());
        }).toList();

        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        OrderEntity order = orders.save(new OrderEntity(request.customerId(), request.channel(),
                request.commune(), request.branchId(), items, total));
        addEvent(order, null, "CREADO", actorFrom(jwt));
        return OrderView.from(order);
    }

    @PatchMapping("/{id}/state")
    @Transactional
    OrderView changeState(@PathVariable UUID id, @Valid @RequestBody ChangeStateRequest request,
                          @AuthenticationPrincipal Jwt jwt) {
        OrderEntity order = find(id);
        String oldState = order.getState();
        String newState = request.state().strip().toUpperCase(Locale.ROOT);
        if (!isAllowedTransition(oldState, newState)) {
            throw new IllegalArgumentException("No se permite cambiar el pedido de " + oldState + " a " + newState + ".");
        }
        order.setState(newState);
        addEvent(order, oldState, newState, actorFrom(jwt));
        return OrderView.from(orders.save(order));
    }

    private PedidoEvent.Actor actorFrom(Jwt jwt) {
        String userId = jwt.getClaimAsString("oid");
        if (userId == null) userId = jwt.getSubject();
        if (userId == null) userId = jwt.getClaimAsString("preferred_username");
        if (userId == null) throw new IllegalArgumentException("El token no identifica al actor.");
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new PedidoEvent.Actor(userId, roles != null && roles.contains("Admin") ? "ADMIN" : "CLIENTE");
    }

    private void addEvent(OrderEntity order, String previousState, String state, PedidoEvent.Actor actor) {
        PedidoEvent event = new PedidoEvent(
                UUID.randomUUID(),
                "PEDIDO_" + state,
                2,
                Instant.now(),
                order.getId().toString(),
                order.getCustomerId(),
                order.getTotal(),
                actor,
                previousState,
                state,
                order.getItems().stream().map(item -> new PedidoEvent.Item(
                        item.getProductId(), item.getName(), item.getCategory(),
                        item.getQuantity(), item.getUnitPrice())).toList(),
                order.getChannel(),
                order.getCommune(),
                order.getBranchId());
        try {
            outbox.save(new OutboxEntry(event.eventId(), event.pedidoId(), "pedido.event",
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar el evento del pedido.", exception);
        }
    }

    private OrderEntity find(UUID id) {
        return orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
    }

    private boolean isAllowedTransition(String from, String to) {
        return switch (from) {
            case "CREADO" -> to.equals("ACEPTADO") || to.equals("CANCELADO");
            case "ACEPTADO" -> to.equals("EN_PREPARACION") || to.equals("CANCELADO");
            case "EN_PREPARACION" -> to.equals("DESPACHADO") || to.equals("CANCELADO");
            case "DESPACHADO" -> to.equals("ENTREGADO");
            default -> false;
        };
    }

    record CreateOrderRequest(
            @NotBlank String customerId,
            @NotEmpty List<@Valid OrderItemRequest> items,
            @NotBlank String actorUserId,
            @NotBlank String channel,
            @NotBlank String commune,
            @NotBlank String branchId) {}

    record OrderItemRequest(@NotNull UUID productId, @Min(1) int quantity) {}
    record ChangeStateRequest(@NotBlank String state, @NotBlank String actorUserId) {}
    record OrderItemView(String productId, String name, String category, int quantity, BigDecimal unitPrice) {}

    record OrderView(UUID id, String customerId, String state, BigDecimal total,
                     String channel, String commune, String branchId, Instant createdAt,
                     Instant updatedAt, List<OrderItemView> items) {
        static OrderView from(OrderEntity order) {
            return new OrderView(order.getId(), order.getCustomerId(), order.getStateValue(),
                    order.getTotal(), order.getChannel(), order.getCommune(), order.getBranchId(),
                    order.getCreatedAt(), order.getUpdatedAt(), order.getItems().stream()
                    .map(item -> new OrderItemView(item.getProductId(), item.getName(),
                            item.getCategory(), item.getQuantity(), item.getUnitPrice())).toList());
        }
    }
}
