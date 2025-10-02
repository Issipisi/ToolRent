package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.ToolUnitRepository;
import com.toolrent.repositories.KardexMovementRepository;
import com.toolrent.config.SecurityConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ToolUnitService {

    private final ToolUnitRepository toolUnitRepository;
    private final KardexMovementRepository kardexMovementRepository;
    private final CustomerService customerService;

    public ToolUnitService(ToolUnitRepository toolUnitRepository,
                           KardexMovementRepository kardexMovementRepository,
                           CustomerService customerService) {
        this.toolUnitRepository = toolUnitRepository;
        this.kardexMovementRepository = kardexMovementRepository;
        this.customerService = customerService;
    }

    @Transactional
    public ToolUnitEntity changeStatus(Long unitId, ToolStatus newStatus) {
        ToolUnitEntity unit = toolUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada"));

        if (unit.getStatus() == newStatus) {
            throw new RuntimeException("La unidad ya está en estado: " + newStatus);
        }

        if (unit.getStatus() == ToolStatus.RETIRED) {
            throw new RuntimeException("La unidad ya fue retirada anteriormente");
        }

        /* ---------- Kardex ---------- */
        MovementType movementType = mapStatusToMovementType(newStatus);
        if (movementType != null) {
            KardexMovementEntity movement = new KardexMovementEntity();
            movement.setCustomer(customerService.getSystemCustomer());
            movement.setToolUnit(unit);
            movement.setMovementType(movementType);
            movement.setDetails("Cambio de estado: " + unit.getStatus() + " → " + newStatus +
                    " - Usuario: " + SecurityConfig.getCurrentUsername());
            kardexMovementRepository.save(movement);
        }

        unit.setStatus(newStatus);
        return toolUnitRepository.save(unit);
    }

    private MovementType mapStatusToMovementType(ToolStatus status) {
        return switch (status) {
            case IN_REPAIR -> MovementType.REPAIR;
            case RETIRED   -> MovementType.RETIRE;
            case AVAILABLE -> MovementType.RE_ENTRY;
            default        -> null; // No registrar otros estados
        };
    }

    /* Buscar una unidad disponible de un grupo */
    public ToolUnitEntity findAvailableUnit(Long toolGroupId) {
        return toolUnitRepository
                .findFirstByToolGroupIdAndStatus(toolGroupId, ToolStatus.AVAILABLE)
                .orElseThrow(() -> new RuntimeException("No hay unidades disponibles"));
    }

    public ToolUnitEntity findByIdUnit(Long unitId) {
        return toolUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));
    }

    public ToolUnitEntity save(ToolUnitEntity unit) {
        return toolUnitRepository.save(unit);
    }
}
