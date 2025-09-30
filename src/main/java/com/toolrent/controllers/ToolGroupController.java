package com.toolrent.controllers;

import com.toolrent.entities.ToolGroupEntity;
import com.toolrent.services.ToolGroupService;
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

    public ToolGroupController(ToolGroupService toolGroupService) {
        this.toolGroupService = toolGroupService;
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
}