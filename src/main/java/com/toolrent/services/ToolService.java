package com.toolrent.services;

import com.toolrent.config.SecurityConfig;
import com.toolrent.entities.*;
import com.toolrent.repositories.KardexMovementRepository;
import com.toolrent.repositories.ToolRepository;
import org.springframework.stereotype.Service;

@Service
public class ToolService {

    private final ToolRepository toolRepository;
    private final KardexMovementRepository kardexMovementRepository;

    public ToolService(ToolRepository toolRepository,
                       KardexMovementRepository kardexMovementRepository) {
        this.toolRepository = toolRepository;
        this.kardexMovementRepository = kardexMovementRepository;
    }

    /* Registrar herramienta con stock variable */
    public ToolEntity registerTool(String name, String category, Double replacementValue,
                                   Double pricePerDay, int stock) {
        if (name == null || name.isBlank() ||
                category == null || category.isBlank() ||
                replacementValue == null) {
            throw new RuntimeException("Nombre, categoría y valor de reposición son obligatorios");
        }
        if (stock < 0) {
            throw new RuntimeException("El stock no puede ser negativo");
        }

        ToolEntity tool = new ToolEntity();
        tool.setName(name);
        tool.setCategory(category);
        tool.setReplacementValue(replacementValue);
        tool.setPricePerDay(pricePerDay);
        tool.setStock(stock);
        tool.setStatus(ToolStatus.AVAILABLE);
        ToolEntity saved = toolRepository.save(tool);

        /* Movimiento Kardex: Alta de herramienta */
        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setTool(saved);
        movement.setCustomer(null); // Alta interna
        movement.setMovementType(MovementType.REGISTRY);

        movement.setDetails("Alta de herramienta: " + saved.getName() + " - Stock inicial: " + stock +
                " - Usuario: " + SecurityConfig.getCurrentUsername());
        kardexMovementRepository.save(movement);

        return saved;
    }

    /* Cambiar estado a AVAILABLE, LOANED o IN_REPAIR */
    public ToolEntity changeStatus(Long id, ToolStatus newStatus) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        // Si pasa a AVAILABLE, incrementamos stock
        if (newStatus == ToolStatus.AVAILABLE) {
            tool.setStock(tool.getStock() + 1);
        }

        tool.setStatus(newStatus);
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
        movement.setCustomer(null);
        movement.setMovementType(MovementType.RETIRE);
        movement.setDetails("Baja de herramienta: " + tool.getName() + " - Usuario: " + SecurityConfig.getCurrentUsername());
        kardexMovementRepository.save(movement);
    }

    /* Modifica el stock de una herramienta (sin cambiar estado) */
    public ToolEntity updateStock(Long id, int delta) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        int newStock = tool.getStock() + delta;
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