package com.toolrent.services;

import com.toolrent.entities.KardexMovementEntity;
import com.toolrent.repositories.KardexMovementRepository;
import org.springframework.stereotype.Service;

@Service
public class KardexMovementService {

    private final KardexMovementRepository kardexMovementRepository;

    public KardexMovementService(KardexMovementRepository kardexMovementRepository) {
        this.kardexMovementRepository = kardexMovementRepository;
    }

    public Iterable<KardexMovementEntity> getAllMovements() {
        return kardexMovementRepository.findAll();
    }
}