package com.toolrent.controllers;

import com.toolrent.entities.TariffEntity;
import com.toolrent.repositories.TariffRepository;
import com.toolrent.services.TariffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tariffs")
@Tag(name = "Tarifa Controller", description = "Endpoints para tarifas del sistema")
public class TariffController {

    private final TariffService tariffService;
    private final TariffRepository tariffRepository;

    public TariffController(TariffService tariffService, TariffRepository tariffRepository) {
        this.tariffService = tariffService;
        this.tariffRepository = tariffRepository;
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Configurar tarifas diarias", description = "Configura tarifa de arriendo y multa por atraso")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarifas actualizadas"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<TariffEntity> updateTariff(@RequestParam Double dailyRentalRate,
                                                     @RequestParam Double dailyFineRate) {
        TariffEntity tariff = tariffService.updateTariff(dailyRentalRate, dailyFineRate);
        return ResponseEntity.ok(tariff);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Obtener tarifa por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarifa encontrada"),
            @ApiResponse(responseCode = "404", description = "Tarifa no encontrada")
    })
    public ResponseEntity<TariffEntity> getTariffById(@PathVariable Long id) {
        TariffEntity tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tariff not found"));
        return ResponseEntity.ok(tariff);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Listar todas las tarifas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarifas encontradas"),
            @ApiResponse(responseCode = "404", description = "Tarifas no encontradas")
    })
    public ResponseEntity<List<TariffEntity>> getAllTariffs() {
        return ResponseEntity.ok(tariffService.getAllTariffs());
    }

}