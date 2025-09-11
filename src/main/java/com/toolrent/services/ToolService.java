package com.toolrent.services;

import com.toolrent.entities.ToolEntity;
import com.toolrent.entities.ToolStatus;
import com.toolrent.repositories.ToolRepository;
import org.springframework.stereotype.Service;

@Service
public class ToolService {

    private final ToolRepository toolRepository;

    public ToolService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    /* Registrar herramienta */
    public ToolEntity registerTool(String name, String category, Double replacementValue, Double pricePerDay) {
        if (name == null || category == null || replacementValue == null) {
            throw new RuntimeException("Nombre, categoría y valor de reposición son obligatorios");
        }
        ToolEntity tool = new ToolEntity();
        tool.setName(name);
        tool.setCategory(category);
        tool.setReplacementValue(replacementValue);
        tool.setPricePerDay(pricePerDay);
        tool.setStock(1);  // Default 1 al registrar, ajustable
        return toolRepository.save(tool);

        // Aquí llamaríamos a KardexService para generar movimiento
    }

    /* Cambiar estado a AVAILABLE, LOANED o IN_REPAIR */
    public ToolEntity changeStatus(Long id, ToolStatus newStatus) {
        if (newStatus == ToolStatus.RETIRED) {
            throw new RuntimeException("Use desactivateTool() para pasar a RETIRED");
        }

        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        // Si pasa a AVAILABLE, incrementamos stock
        if (newStatus == ToolStatus.AVAILABLE) {
            tool.setStock(tool.getStock() + 1);
        }

        tool.setStatus(newStatus);
        return toolRepository.save(tool);
    }

    /* Para dar de baja una herramienta */
    public void desactivateTool(Long id) {
        ToolEntity tool = toolRepository.findById(id).orElseThrow(() -> new RuntimeException("Tool not found"));
        tool.setStatus(ToolStatus.RETIRED);
        toolRepository.save(tool);

        // Aquí llamaríamos a KardexService para movimiento de baja (en Día 4)
    }

    public Iterable<ToolEntity> getAllTools() {
        return toolRepository.findAll();
    }
}