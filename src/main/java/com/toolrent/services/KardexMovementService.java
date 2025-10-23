package com.toolrent.services;

import com.toolrent.config.SecurityConfig;
import com.toolrent.entities.KardexMovementEntity;
import com.toolrent.entities.MovementType;
import com.toolrent.entities.ToolGroupEntity;
import com.toolrent.repositories.KardexMovementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KardexMovementService {

    private final KardexMovementRepository kardexMovementRepository;
    private final CustomerService customerService;

    public KardexMovementService(KardexMovementRepository kardexMovementRepository,
                                 CustomerService customerService) {
        this.kardexMovementRepository = kardexMovementRepository;
        this.customerService = customerService;
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

    //Kardex Registro
    public void saveRegistryKardex(ToolGroupEntity group, int stock) {
        if (stock == 0) return;
        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setCustomer(customerService.getSystemCustomer());
        movement.setToolUnit(group.getUnits().get(0));
        movement.setMovementType(MovementType.REGISTRY);
        movement.setDetails("Creación de grupo: " + group.getName() +
                " - Stock inicial: " + stock +
                " - Usuario: " + SecurityConfig.getCurrentUsername());
        kardexMovementRepository.save(movement);
    }
}