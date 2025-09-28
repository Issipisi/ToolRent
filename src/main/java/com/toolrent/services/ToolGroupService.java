package com.toolrent.services;

import com.toolrent.config.SecurityConfig;
import com.toolrent.entities.*;
import com.toolrent.repositories.*;
import org.springframework.stereotype.Service;

@Service
public class ToolGroupService {

    private final ToolGroupRepository toolGroupRepository;
    private final KardexMovementRepository kardexMovementRepository;
    private final CustomerRepository customerRepository;

    public ToolGroupService(ToolGroupRepository toolGroupRepository,
                            KardexMovementRepository kardexMovementRepository,
                            CustomerRepository customerRepository) {
        this.toolGroupRepository = toolGroupRepository;
        this.kardexMovementRepository = kardexMovementRepository;
        this.customerRepository = customerRepository;
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
        CustomerEntity systemCustomer = getSystemCustomer();
        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setToolUnit(group.getUnits().get(0));
        movement.setCustomer(systemCustomer);
        movement.setMovementType(MovementType.REGISTRY);
        movement.setDetails("Creación de grupo: " + group.getName() +
                " - Stock inicial: " + stock +
                " - Usuario: " + SecurityConfig.getCurrentUsername());
        kardexMovementRepository.save(movement);
    }


    private CustomerEntity getSystemCustomer() {

        return customerRepository.findByEmail("system@toolrent.com")
                .orElseGet(() -> {
                    CustomerEntity sys = new CustomerEntity();
                    sys.setName("Sistema");
                    sys.setRut("0-0");
                    sys.setEmail("system@toolrent.com");
                    sys.setPhone("000");
                    sys.setStatus(CustomerStatus.ACTIVE);
                    return customerRepository.save(sys);
                });
    }

    public Iterable<ToolGroupEntity> getAllToolGroups() {
        return toolGroupRepository.findAll();
    }


}