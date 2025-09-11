package com.orden_pago.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.orden_pago.demo.model.OrdenItem;
import com.orden_pago.demo.repository.OrdenItemRepository;

@Service
public class OrdenItemService {

    private final OrdenItemRepository ordenItemRepository;

    public OrdenItemService(OrdenItemRepository ordenItemRepository) {
        this.ordenItemRepository = ordenItemRepository;
    }

    public void crearOrdenItem(OrdenItem ordenItem) {
        ordenItemRepository.save(ordenItem);
    }

    public List<OrdenItem> obtenerTodosLosOrdenItems() {
        return ordenItemRepository.findAll();
    }

    public Optional<OrdenItem> obtenerOrdenItemPorId(Long id) {
        return ordenItemRepository.findById(id);
    }

    public void eliminarOrdenItem(Long id) {
        ordenItemRepository.deleteById(id);
    }
}
