package com.orden_pago.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orden_pago.demo.model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long>{
    
}
