package com.orden_pago.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orden_pago.demo.model.Orden;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long>{
    
}
