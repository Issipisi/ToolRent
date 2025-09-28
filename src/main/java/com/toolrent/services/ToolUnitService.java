package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.ToolUnitRepository;
import org.springframework.stereotype.Service;

@Service
public class ToolUnitService {

    private final ToolUnitRepository toolUnitRepository;

    public ToolUnitService(ToolUnitRepository toolUnitRepository) {
        this.toolUnitRepository = toolUnitRepository;
    }

    /* Cambiar estado de una unidad */
    public ToolUnitEntity changeStatus(Long unitId, ToolStatus newStatus) {
        ToolUnitEntity unit = toolUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada"));

        if (unit.getStatus() == newStatus) {
            throw new RuntimeException("La unidad ya está en estado: " + newStatus);
        }

        unit.setStatus(newStatus);
        return toolUnitRepository.save(unit);
    }

    /* Buscar una unidad disponible de un grupo */
    public ToolUnitEntity findAvailableUnit(Long toolGroupId) {
        return toolUnitRepository
                .findFirstByToolGroupIdAndStatus(toolGroupId, ToolStatus.AVAILABLE)
                .orElseThrow(() -> new RuntimeException("No hay unidades disponibles"));
    }
}
