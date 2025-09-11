package com.toolrent.controllers;

import com.toolrent.entities.KardexMovementEntity;
import com.toolrent.services.KardexMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Iterable<KardexMovementEntity>> getAllMovements() {
        return ResponseEntity.ok(kardexMovementService.getAllMovements());
    }
}
