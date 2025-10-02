package com.toolrent.controllers;

import com.toolrent.entities.ToolGroupEntity;
import com.toolrent.entities.ToolStatus;
import com.toolrent.entities.ToolUnitEntity;
import com.toolrent.services.ToolGroupService;
import com.toolrent.services.ToolUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tools")
@Tag(name = "Tool Group Controller", description = "Gestión de grupos de herramientas")
public class ToolGroupController {

    private final ToolGroupService toolGroupService;
    private final ToolUnitService toolUnitService;

    public ToolGroupController(ToolGroupService toolGroupService,
                               ToolUnitService toolUnitService) {
        this.toolGroupService = toolGroupService;
        this.toolUnitService = toolUnitService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar nuevo grupo de herramientas con unidades")
    public ResponseEntity<ToolGroupEntity> registerToolGroup(
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam Double replacementValue,
            @RequestParam Double pricePerDay,
            @RequestParam int stock) {

        ToolGroupEntity group = toolGroupService.registerToolGroup(name, category, replacementValue, pricePerDay, stock);
        return ResponseEntity.ok(group);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Listar todos los grupos")
    public ResponseEntity<Iterable<ToolGroupEntity>> getAllToolGroups() {
        return ResponseEntity.ok(toolGroupService.getAllToolGroups());
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Listar grupos con unidades disponibles")
    public ResponseEntity<List<ToolGroupEntity>> getAvailableToolGroups() {
        return ResponseEntity.ok(toolGroupService.getToolGroupsWithAvailableUnits());
    }


    @PutMapping("/{id}/tariff")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar tarifa de un grupo de herramientas")
    public ResponseEntity<ToolGroupEntity> updateTariff(
            @PathVariable Long id,
            @RequestParam Double dailyRentalRate,
            @RequestParam Double dailyFineRate) {

        ToolGroupEntity group = toolGroupService.findById(id);
        group.getTariff().setDailyRentalRate(dailyRentalRate);
        group.getTariff().setDailyFineRate(dailyFineRate);
        return ResponseEntity.ok(toolGroupService.save(group));
    }

    /* ---------- Listar todas las unidades con detalles ---------- */
    @GetMapping("/units")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Obtener todas las herramientas de forma unitaria")
    public ResponseEntity<List<ToolUnitEntity>> getAllUnitsWithDetails() {
        return ResponseEntity.ok(toolGroupService.findAllUnitsWithDetails());
    }


    @PutMapping("/units/{unitId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar el estado de una herramienta")
    public ResponseEntity<ToolUnitEntity> changeUnitStatus(
            @PathVariable Long unitId,
            @RequestParam ToolStatus newStatus) {
        
        ToolUnitEntity updated = toolUnitService.changeStatus(unitId, newStatus);
        return ResponseEntity.ok(updated);
    }


    @GetMapping("/{id}/real-stock")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Obtener el stock real")
    public ResponseEntity<Long> getRealStock(@PathVariable Long id) {
        return ResponseEntity.ok(toolGroupService.getRealStock(id));
    }


    /* ---------- NUEVO: resolver reparación ---------- */
    @PutMapping("/units/{unitId}/repair-resolution")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolver reparación: disponible o retirada")
    public ResponseEntity<ToolUnitEntity> resolveRepair(
            @PathVariable Long unitId,
            @RequestParam boolean retire) {

        ToolStatus target = retire ? ToolStatus.RETIRED : ToolStatus.AVAILABLE;
        ToolUnitEntity updated = toolUnitService.changeStatus(unitId, target);
        return ResponseEntity.ok(updated);
    }



}