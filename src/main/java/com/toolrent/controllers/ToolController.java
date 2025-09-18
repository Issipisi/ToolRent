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

    /* ---------- REGISTRO ---------- */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar una nueva herramienta",
            description = "Registra una herramienta con datos básicos y stock deseado. Genera movimiento Kardex.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Herramienta registrada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ToolEntity> registerTool(@RequestParam String name,
                                                   @RequestParam String category,
                                                   @RequestParam Double replacementValue,
                                                   @RequestParam Double pricePerDay,
                                                   @RequestParam int stock) {
        ToolEntity tool = toolService.registerTool(name, category, replacementValue, pricePerDay, stock);
        return ResponseEntity.ok(tool);
    }

    /* ---------- CAMBIO DE ESTADO ---------- */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de una herramienta",
            description = "Permite mover la herramienta entre AVAILABLE, LOANED o IN_REPAIR. No genera Kardex.")
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

    /* ---------- BAJA ---------- */
    @PutMapping("/{id}/desactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dar de baja una herramienta",
            description = "Cambia el estado a RETIRED y genera movimiento Kardex.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Herramienta dada de baja"),
            @ApiResponse(responseCode = "404", description = "Herramienta no encontrada"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<Void> deactivateTool(@PathVariable Long id) {
        toolService.desactivateTool(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ajustar stock",
            description = "Suma o resta unidades al stock actual sin cambiar estado(correcciones).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock actualizado"),
            @ApiResponse(responseCode =  "400", description = "Stock resultante negativo"),
            @ApiResponse(responseCode = "404", description = "Herramienta no encontrada"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ToolEntity> updateStock(
            @PathVariable Long id,
            @RequestParam int newStock) {

        ToolEntity tool = toolService.updateStock(id, newStock);
        return ResponseEntity.ok(tool);
    }

    /* ---------- LISTADO ---------- */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Listar todas las herramientas",
            description = "Retorna todas las herramientas para reportes o consulta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de herramientas"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<Iterable<ToolEntity>> getAllTools() {
        return ResponseEntity.ok(toolService.getAllTools());
    }

    @PatchMapping("/{id}/replacement-value")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar valor de reposición",
            description = "Permite al administrador modificar el valor de reposición de una herramienta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Valor actualizado"),
            @ApiResponse(responseCode = "400", description = "Valor inválido"),
            @ApiResponse(responseCode = "404", description = "Herramienta no encontrada"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ToolEntity> updateReplacementValue(
            @PathVariable Long id,
            @RequestParam Double value) {

        ToolEntity updated = toolService.updateReplacementValue(id, value);
        return ResponseEntity.ok(updated);
    }
}