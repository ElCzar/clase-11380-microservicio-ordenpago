package com.orden_pago.demo.controller;

import com.orden_pago.demo.dto.AddItemRequest;
import com.orden_pago.demo.dto.CartHistoryDTO;
import com.orden_pago.demo.dto.PaymentRequest;
import com.orden_pago.demo.dto.PaymentResponse;
import com.orden_pago.demo.dto.ServiceResponseDTO;
import com.orden_pago.demo.dto.UpdateQuantityRequest;
import com.orden_pago.demo.model.Cart;
import com.orden_pago.demo.model.CartItem;
import com.orden_pago.demo.service.CartService;
import com.orden_pago.demo.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final PaymentService paymentService;

    /**
     * GET /api/cart - Obtener carrito actual
     */
    @GetMapping
    public ResponseEntity<Cart> getCurrentCart(Authentication authentication) {
        try {
            log.info("Solicitando carrito actual");
            Cart cart = cartService.getCurrentCart(authentication);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            log.error("Error obteniendo carrito actual: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/cart/items - Agregar item al carrito
     */
    @PostMapping("/items")
    public ResponseEntity<CartItem> addItem(
            @Valid @RequestBody AddItemRequest request,
            Authentication authentication) {
        try {
            log.info("Agregando item al carrito: servicio={}, cantidad={}",
                    request.getServiceId(), request.getQuantity());

            CartItem cartItem = cartService.addItemToCart(
                    authentication,
                    request.getServiceId(),
                    request.getQuantity());

            return ResponseEntity.status(HttpStatus.CREATED).body(cartItem);
        } catch (RuntimeException e) {
            log.error("Error agregando item al carrito: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error interno agregando item al carrito: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/cart/items/{itemId} - Actualizar cantidad
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartItem> updateQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        try {
            log.info("Actualizando cantidad del item: {}, nueva cantidad: {}",
                    itemId, request.getQuantity());

            CartItem updatedItem = cartService.updateItemQuantity(itemId, request.getQuantity());
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            log.error("Error actualizando cantidad del item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error interno actualizando cantidad del item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/cart/items/{itemId} - Remover item
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable UUID itemId) {
        try {
            log.info("Removiendo item del carrito: {}", itemId);
            cartService.removeItemFromCart(itemId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error removiendo item del carrito: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error interno removiendo item del carrito: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/cart - Vaciar carrito
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        try {
            log.info("Vaciando carrito");
            cartService.clearCart(authentication);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error vaciando carrito: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/cart/history - Obtener historial de carritos
     */
    @GetMapping("/history")
    public ResponseEntity<List<CartHistoryDTO>> getCartHistory(Authentication authentication) {
        try {
            log.info("Obteniendo historial de carritos");
            List<CartHistoryDTO> history = cartService.getUserCartHistory(authentication);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error obteniendo historial de carritos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/cart/checkout - Procesar pago del carrito actual
     * Endpoint integrado que obtiene el carrito actual y procesa el pago
     */
    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponse> checkoutCart(
            @Valid @RequestBody PaymentRequest paymentRequest,
            Authentication authentication) {
        try {
            log.info("🛒 Iniciando checkout del carrito actual");

            // 1. Obtener carrito actual
            Cart currentCart = cartService.getCurrentCart(authentication);

            // 2. Validar que el carrito no esté vacío
            if (currentCart.getItems() == null || currentCart.getItems().isEmpty()) {
                log.warn("❌ Intento de pago con carrito vacío");
                return ResponseEntity.badRequest()
                        .body(PaymentResponse.builder()
                                .status(com.orden_pago.demo.enums.PaymentStatus.FAILED)
                                .message("El carrito está vacío. Agregue items antes de proceder al pago.")
                                .processedAt(java.time.LocalDateTime.now())
                                .build());
            }

            // 3. Calcular total del carrito
            BigDecimal total = currentCart.getItems().stream()
                    .map(CartItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("💰 Total del carrito a pagar: ${}", total);

            // 4. Establecer el cartId en el request de pago
            paymentRequest.setCartId(currentCart.getId());

            // 5. Procesar el pago
            PaymentResponse response = paymentService.processPayment(paymentRequest, authentication);

            // 6. Log del resultado
            if (response.getStatus() == com.orden_pago.demo.enums.PaymentStatus.COMPLETED) {
                log.info("✅ Pago procesado exitosamente - TransactionId: {}, Monto: ${}",
                        response.getTransactionId(), response.getAmount());
            } else {
                log.warn("⚠️ Pago falló - Estado: {}, Mensaje: {}",
                        response.getStatus(), response.getMessage());
            }

            // 7. Retornar respuesta con status apropiado
            HttpStatus status = switch (response.getStatus()) {
                case COMPLETED -> HttpStatus.OK;
                case FAILED -> HttpStatus.BAD_REQUEST;
                case PENDING -> HttpStatus.ACCEPTED;
            };

            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("❌ Error en checkout del carrito: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.builder()
                            .status(com.orden_pago.demo.enums.PaymentStatus.FAILED)
                            .message("Error interno durante el procesamiento del pago")
                            .processedAt(java.time.LocalDateTime.now())
                            .build());
        }
    }

    /**
     * GET /api/cart/items/available - Listar servicios disponibles desde el repositorio
     * Endpoint que muestra servicios que fueron previamente procesados desde Kafka
     */
    @GetMapping("/items/available")
    public ResponseEntity<List<ServiceResponseDTO>> getAvailableServices() {
        try {
            log.info("📋 Listando servicios disponibles desde el repositorio");

            List<ServiceResponseDTO> availableServices = cartService.getAvailableServicesFromRepository();
            
            if (availableServices.isEmpty()) {
                log.warn("⚠️ No se encontraron servicios disponibles en el repositorio");
                return ResponseEntity.ok(availableServices); // Devolver lista vacía con 200 OK
            }
            
            log.info("✅ Encontrados {} servicios disponibles", availableServices.size());
            return ResponseEntity.ok(availableServices);

        } catch (Exception e) {
            log.error("❌ Error obteniendo servicios disponibles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}