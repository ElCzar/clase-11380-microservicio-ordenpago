package com.orden_pago.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.orden_pago.demo.model.OrdenItem;

public interface OrdenItemRepository extends JpaRepository<OrdenItem, Long>{
    
}
