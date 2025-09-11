package com.orden_pago.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.orden_pago.demo.model.Orden;
import com.orden_pago.demo.repository.OrdenRepository;

@Service
public class OrdenService {

    private final OrdenRepository ordenRepository;

    public OrdenService(OrdenRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    public void crearOrden(Orden orden) {
        ordenRepository.save(orden);
    }

    public List<Orden> obtenerTodasLasOrdenes() {
        return ordenRepository.findAll();
    }

    public Optional<Orden> obtenerOrdenPorId(Long id) {
        return ordenRepository.findById(id);
    }

    public void eliminarOrden(Long id) {
        ordenRepository.deleteById(id);
    }
}
