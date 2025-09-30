package com.toolrent.services;

import com.toolrent.entities.KardexMovementEntity;
import com.toolrent.repositories.KardexMovementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KardexMovementService {

    private final KardexMovementRepository kardexMovementRepository;

    public KardexMovementService(KardexMovementRepository kardexMovementRepository) {
        this.kardexMovementRepository = kardexMovementRepository;
    }

    //Obtener Todos los movimientos
    public List<KardexMovementEntity> getAllMovements() {
        return kardexMovementRepository.findAllWithDetails();
    }

    // Filtro por herramienta
    public List<KardexMovementEntity> findByToolGroupId(Long toolGroupId) {
        return kardexMovementRepository.findByToolGroupId(toolGroupId);
    }

    // Filtro por rango de fecha
    public List<KardexMovementEntity> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return kardexMovementRepository.findByDateRange(from, to);
    }
}