package com.toolrent.services;

import com.toolrent.config.SecurityConfig;
import com.toolrent.entities.*;
import com.toolrent.repositories.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolGroupService {

    private final ToolGroupRepository toolGroupRepository;
    private final KardexMovementRepository kardexMovementRepository;
    private final CustomerService customerService;
    private final ToolUnitRepository toolUnitRepository;

    public ToolGroupService(ToolGroupRepository toolGroupRepository,
                            KardexMovementRepository kardexMovementRepository,
                            CustomerService customerService,
                            ToolUnitRepository toolUnitRepository) {
        this.toolGroupRepository = toolGroupRepository;
        this.kardexMovementRepository = kardexMovementRepository;
        this.customerService = customerService;
        this.toolUnitRepository = toolUnitRepository;
    }

    /* Crear grupo + unidades */
    public ToolGroupEntity registerToolGroup(String name, String category, Double replacementValue,
                                             Double pricePerDay, int stock) {

        if (name == null || name.isBlank() || category == null || category.isBlank() || replacementValue == null) {
            throw new RuntimeException("Nombre, categoría y valor de reposición son obligatorios");
        }
        if (stock < 0) {
            throw new RuntimeException("El stock no puede ser negativo");
        }

        // 1. Crear tarifa
        TariffEntity tariff = new TariffEntity();
        tariff.setDailyRentalRate(pricePerDay);
        tariff.setDailyFineRate(2500.0);

        // 2. Crear grupo
        ToolGroupEntity group = new ToolGroupEntity();
        group.setName(name);
        group.setCategory(category);
        group.setReplacementValue(replacementValue);
        group.setTariff(tariff);

        // 3. Generar unidades (llamada al servicio de unidades)
        for (int i = 0; i < stock; i++) {
            ToolUnitEntity unit = new ToolUnitEntity();
            unit.setToolGroup(group);
            unit.setStatus(ToolStatus.AVAILABLE);
            group.getUnits().add(unit);
        }

        ToolGroupEntity saved = toolGroupRepository.save(group);
        saveRegistryKardex(saved, stock);

        return saved;
    }

    private void saveRegistryKardex(ToolGroupEntity group, int stock) {
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




    public Iterable<ToolGroupEntity> getAllToolGroups() {
        return toolGroupRepository.findAll();
    }

    public List<ToolGroupEntity> getToolGroupsWithAvailableUnits() {
        return toolGroupRepository.findAll().stream()
                .filter(g -> g.getUnits().stream().anyMatch(u -> u.getStatus() ==
                        ToolStatus.AVAILABLE))
                .toList();
    }

    public ToolGroupEntity findById(Long id) {
        return toolGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ToolGroup not found"));
    }

    public ToolGroupEntity save(ToolGroupEntity group) {
        return toolGroupRepository.save(group);
    }

    public List<ToolUnitEntity> findAllUnitsWithDetails() {
        return toolUnitRepository.findAllWithToolGroup();
    }

    public ToolUnitEntity save(ToolUnitEntity unit) {
        return toolUnitRepository.save(unit);
    }

    public long getRealStock(Long toolGroupId) {
        return toolUnitRepository.countByToolGroupIdAndStatusNot(toolGroupId, ToolStatus.RETIRED);
    }
}