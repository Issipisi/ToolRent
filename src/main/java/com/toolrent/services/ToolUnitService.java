package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.LoanRepository;
import com.toolrent.repositories.ToolUnitRepository;
import com.toolrent.repositories.KardexMovementRepository;
import com.toolrent.config.SecurityConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class ToolUnitService {

    private final ToolUnitRepository toolUnitRepository;
    private final KardexMovementRepository kardexMovementRepository;
    private final CustomerService customerService;
    private final LoanRepository loanRepository;

    public ToolUnitService(ToolUnitRepository toolUnitRepository,
                           KardexMovementRepository kardexMovementRepository,
                           CustomerService customerService,
                           LoanRepository loanRepository) {
        this.toolUnitRepository = toolUnitRepository;
        this.kardexMovementRepository = kardexMovementRepository;
        this.customerService = customerService;
        this.loanRepository = loanRepository;
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

    // Movimientos posibles del kárdex para herramientas
    private MovementType mapStatusToMovementType(ToolStatus status) {
        return switch (status) {
            case IN_REPAIR -> MovementType.REPAIR;
            case RETIRED   -> MovementType.RETIRE;
            case AVAILABLE -> MovementType.RE_ENTRY;
            default        -> null; // No registrar otros estados
        };
    }

    // Buscar una unidad disponible de un grupo
    public ToolUnitEntity findAvailableUnit(Long toolGroupId) {
        return toolUnitRepository
                .findFirstByToolGroupIdAndStatus(toolGroupId, ToolStatus.AVAILABLE)
                .orElseThrow(() -> new RuntimeException("No hay unidades disponibles"));
    }

    // Retira una unidad que está en reparación y carga el valor de reposición
    // como deuda al último préstamo devuelto de esa unidad.
    @Transactional
    public void retireFromRepair(Long unitId) {
        ToolUnitEntity unit = findById(unitId);
        if (unit.getStatus() != ToolStatus.IN_REPAIR) {
            throw new RuntimeException("La unidad no está en reparación");
        }

        // Último préstamo devuelto de esta unidad
        LoanEntity loan = loanRepository
                .findTopByToolUnitIdAndReturnDateIsNotNullOrderByReturnDateDesc(unitId)
                .orElseThrow(() -> new RuntimeException("No se encontró préstamo devuelto para esta unidad"));

        // Cargar valor de reposición como deuda
        loan.setDamageCharge(unit.getToolGroup().getReplacementValue());
        loanRepository.save(loan);

        // Cambiar estado
        changeStatus(unitId, ToolStatus.RETIRED);
    }


    public ToolUnitEntity findById(Long unitId) {
        return toolUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada"));
    }

    public List<ToolUnitEntity> findAllUnitsWithDetails() {
        return toolUnitRepository.findAllWithToolGroup();
    }

    public ToolUnitEntity save(ToolUnitEntity unit) {
        return toolUnitRepository.save(unit);
    }

    public long getRealStock(Long toolGroupId) {
        return toolUnitRepository.countByToolGroupIdAndStatus(toolGroupId, ToolStatus.AVAILABLE);
    }
}
