package com.toolrent.controllers;

import com.toolrent.dto.CustomerDebtDTO;
import com.toolrent.dto.LoanActiveDTO;
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
    @Operation(summary = "Listar préstamos activos", description = "Muestra todos los prestamos activos.")
    public ResponseEntity<List<LoanActiveDTO>> getActiveLoans(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (from == null) from = LocalDateTime.now().minusMonths(1);
        if (to == null)   to = LocalDateTime.now();

        return ResponseEntity.ok(reportService.getActiveLoans(from, to));
    }

    @GetMapping("/top-tools")
    @Operation(summary = "Listar ranking de Herramientas", description = "Ranking de herramientas más prestadas.")
    public ResponseEntity<List<Map<String, Object>>> getTopTools(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (from == null) from = LocalDateTime.now().minusMonths(1);
        if (to == null)   to = LocalDateTime.now();

        return ResponseEntity.ok(reportService.getTopTools(from, to));
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

    @GetMapping("/customers-with-debt")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Listar clientes con deudas", description = "Retorna clientes con deudas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de clientes"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<CustomerDebtDTO>> getCustomersWithDebt() {
        return ResponseEntity.ok(reportService.getCustomersWithDebt(LocalDateTime.now()));
    }
}