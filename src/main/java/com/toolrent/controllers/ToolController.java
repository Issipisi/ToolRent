package com.toolrent.controllers;

import com.toolrent.entities.ToolEntity;
import com.toolrent.entities.ToolStatus;
import com.toolrent.services.ToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tools")
@Tag(name = "Tools Controller", description = "Endpoints para Herramientas")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar una nueva herramienta", description = "Registra una herramienta con datos básicos " +
            "(nombre, categoría, valor de reposición, precio por día)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Herramienta registrada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ToolEntity> registerTool(@RequestParam String name,
                                                   @RequestParam String category,
                                                   @RequestParam Double replacementValue,
                                                   @RequestParam Double pricePerDay) {
        ToolEntity tool = toolService.registerTool(name, category, replacementValue, pricePerDay);
        return ResponseEntity.ok(tool);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de una herramienta",
            description = "Permite mover la herramienta entre AVAILABLE, LOANED o IN_REPAIR.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado inválido o RETIRED"),
            @ApiResponse(responseCode = "404", description = "Herramienta no encontrada"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ToolEntity> changeStatus(
            @PathVariable Long id,
            @RequestParam ToolStatus newStatus) {

        ToolEntity tool = toolService.changeStatus(id, newStatus);
        return ResponseEntity.ok(tool);
    }

    @PutMapping("/{id}/desactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dar de baja una herramienta", description = "Cambia el estado a 'RETIRED' para " +
            "herramientas dañadas, solo para Admin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Herramienta dada de baja"),
            @ApiResponse(responseCode = "404", description = "Herramienta no encontrada"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<Void> deactivateTool(@PathVariable Long id) {
        toolService.desactivateTool(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Listar todas las herramientas", description = "Retorna todas las herramientas para reportes o " +
            "consulta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de herramientas"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<Iterable<ToolEntity>> getAllTools() {
        return ResponseEntity.ok(toolService.getAllTools());
    }
}