package com.orden_pago.demo.controller;

import com.orden_pago.demo.dto.AddItemRequest;
import com.orden_pago.demo.dto.UpdateQuantityRequest;
import com.orden_pago.demo.model.Cart;
import com.orden_pago.demo.model.CartItem;
import com.orden_pago.demo.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

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
    public ResponseEntity<?> getCartHistory(Authentication authentication) {
        try {
            log.info("Obteniendo historial de carritos");
            var history = cartService.getUserCartHistory(authentication);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error obteniendo historial de carritos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}