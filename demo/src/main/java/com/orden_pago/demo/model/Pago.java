package com.orden_pago.demo.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import org.springframework.data.annotation.Id;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
@Entity
@Table(name = "pagos")
public class Pago {
    @Id 
    private Long id;

    private Double monto;

    private String metodoPago;

    private String estado;

    private Long ordenId;
}
