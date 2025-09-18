package com.toolrent.services;

import com.toolrent.config.SecurityConfig;
import com.toolrent.entities.*;
import com.toolrent.repositories.CustomerRepository;
import com.toolrent.repositories.KardexMovementRepository;
import com.toolrent.repositories.TariffRepository;
import com.toolrent.repositories.ToolRepository;
import org.springframework.stereotype.Service;


@Service
public class ToolService {

    private final ToolRepository toolRepository;
    private final KardexMovementRepository kardexMovementRepository;
    private final CustomerRepository customerRepository;
    private final TariffRepository tariffRepository;

    public ToolService(ToolRepository toolRepository,
                       KardexMovementRepository kardexMovementRepository,
                       CustomerRepository customerRepository, TariffRepository tariffRepository){
        this.toolRepository = toolRepository;
        this.kardexMovementRepository = kardexMovementRepository;
        this.customerRepository = customerRepository;
        this.tariffRepository = tariffRepository;
    }

    private CustomerEntity getSystemCustomer() {
        return customerRepository.findByEmail("system@toolrent.com")
                .orElseThrow(() -> new RuntimeException("Cliente sistema no configurado"));
    }

    public ToolEntity registerTool(String name, String category, Double replacementValue,
                                   Double pricePerDay, int stock) {

        /* validaciones */
        if (name == null || name.isBlank() || category == null || category.isBlank() || replacementValue == null) {
            throw new RuntimeException("Nombre, categoría y valor de reposición son obligatorios");
        }
        if (stock < 0) {
            throw new RuntimeException("El stock no puede ser negativo");
        }

        /* 1. Crear la tarifa (sin guardarla aún) */
        TariffEntity tariff = new TariffEntity();
        tariff.setDailyRentalRate(pricePerDay);
        tariff.setDailyFineRate(1_000.0);

        /* 2. Crear la herramienta y asignarle la tarifa */
        ToolEntity tool = new ToolEntity();
        tool.setName(name);
        tool.setCategory(category);
        tool.setReplacementValue(replacementValue);
        tool.setPricePerDay(pricePerDay);
        tool.setStock(stock);
        tool.setStatus(ToolStatus.AVAILABLE);
        tool.setTariff(tariff);          // cascada se encargará

        /* 3. Guardar la herramienta (con cascada guarda la tarifa) */
        ToolEntity saved = toolRepository.save(tool);

        /* 4. Kardex */
        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setTool(saved);
        movement.setCustomer(getSystemCustomer());
        movement.setMovementType(MovementType.REGISTRY);
        movement.setDetails("Creación de herramienta: " + saved.getName() +
                " - Stock inicial: " + stock +
                " - Usuario: " + SecurityConfig.getCurrentUsername());
        kardexMovementRepository.save(movement);

        return saved;
    }

    /* Cambiar estado a AVAILABLE, LOANED o IN_REPAIR */
    public ToolEntity changeStatus(Long id, ToolStatus newStatus) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        // Validar si el estado nuevo es igual al actual
        if (tool.getStatus() == newStatus) {
            throw new RuntimeException("La herramienta ya se encuentra en estado: " + newStatus);
        }

        // Si pasa a AVAILABLE, incrementamos stock
        if (newStatus == ToolStatus.AVAILABLE) {
            tool.setStock(tool.getStock() + 1);
        }

        tool.setStatus(newStatus);
        return toolRepository.save(tool);
    }


    // Modificar valor de reposición
    public ToolEntity updateReplacementValue(Long toolId, Double newValue) {
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        if (newValue == null || newValue <= 0) {
            throw new RuntimeException("El valor de reposición debe ser mayor a cero");
        }

        tool.setReplacementValue(newValue);
        return toolRepository.save(tool);
    }

    /* Dar de baja */
    public void desactivateTool(Long id) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));
        tool.setStatus(ToolStatus.RETIRED);
        toolRepository.save(tool);

        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setTool(tool);
        movement.setCustomer(getSystemCustomer());
        movement.setMovementType(MovementType.RETIRE);
        movement.setDetails("Baja de herramienta: " + tool.getName() + " - Usuario: " + SecurityConfig.getCurrentUsername());
        kardexMovementRepository.save(movement);
    }

    /* Modifica el stock de una herramienta (sin cambiar estado) */
    public ToolEntity updateStock(Long id, int newStock) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        if (newStock < 0) {
            throw new RuntimeException("Stock resultante no puede ser negativo");
        }

        tool.setStock(newStock);
        return toolRepository.save(tool);
    }

    public Iterable<ToolEntity> getAllTools() {
        return toolRepository.findAll();
    }
}