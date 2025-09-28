package com.toolrent.controllers;

import com.toolrent.entities.LoanEntity;
import com.toolrent.entities.CustomerEntity;
import com.toolrent.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reporte Controller", description = "Endpoints para Reportes")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/active-loans")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Listar préstamos activos y su estado", description = "Retorna préstamos vigentes o atrasados" +
            " con filtro por fechas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de préstamos"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<LoanEntity>> getActiveLoans(@RequestParam LocalDateTime from, @RequestParam LocalDateTime to) {
        return ResponseEntity.ok(reportService.getActiveLoans(from, to));
    }

    @GetMapping("/overdue-customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Listar clientes con atrasos", description = "Retorna clientes con préstamos atrasados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de clientes"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<CustomerEntity>> getOverdueCustomers() {
        return ResponseEntity.ok(reportService.getOverdueCustomers());
    }

    @GetMapping("/top-tools")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<Map<String, Object>>> getTopTools(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reportService.getTopTools(from, to));
    }
}