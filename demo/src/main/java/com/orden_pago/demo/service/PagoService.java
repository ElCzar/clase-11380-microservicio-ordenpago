package com.orden_pago.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.orden_pago.demo.model.Pago;
import com.orden_pago.demo.repository.PagoRepository;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;

    public PagoService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    public void crearPago(Pago pago) {
        pagoRepository.save(pago);
    }

    public List<Pago> obtenerTodosLosPagos() {
        return pagoRepository.findAll();
    }

    public Optional<Pago> obtenerPagoPorId(Long id) {
        return pagoRepository.findById(id);
    }

    public void eliminarPago(Long id) {
        pagoRepository.deleteById(id);
    }
}
