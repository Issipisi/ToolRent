package com.toolrent.controllers;

import com.toolrent.entities.KardexMovementEntity;
import com.toolrent.services.KardexMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/kardex")
@Tag(name = "Kardex Controller", description = "Endpoints para Kardex")
public class KardexMovementController {

    private final KardexMovementService kardexMovementService;

    public KardexMovementController(KardexMovementService kardexMovementService) {
        this.kardexMovementService = kardexMovementService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Listar movimientos del Kardex", description = "Retorna todos los movimientos para reportes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de movimientos"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<KardexMovementEntity>> getAllMovements() {
        return ResponseEntity.ok(kardexMovementService.getAllMovements());
    }

    /* RF5.2 – historial por herramienta */
    @GetMapping("/by-tool")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Historial por herramienta")
    public ResponseEntity<List<KardexMovementEntity>> getByTool(
            @RequestParam Long toolGroupId) {
        return ResponseEntity.ok(kardexMovementService.findByToolGroupId(toolGroupId));
    }

    /* RF5.3 – movimientos por rango de fechas */
    @GetMapping("/by-range")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Movimientos por rango ")
    public ResponseEntity<List<KardexMovementEntity>> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kardexMovementService.findByDateRange(from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
    }

}
