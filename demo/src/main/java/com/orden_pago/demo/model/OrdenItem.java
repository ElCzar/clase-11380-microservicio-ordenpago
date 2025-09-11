package com.orden_pago.demo.model;

import org.springframework.data.annotation.Id;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
@Entity
@Table(name = "orden_items")
public class OrdenItem {

    @Id
    private Long id;

    private String nombreProducto;

    private Integer cantidad;

    private Double precioUnitario;

    private Double subtotal;
    
    private Long ordenId;

    private Long productoId;
}
