package com.toolrent.controllers;

import com.toolrent.config.LoanActiveDTO;
import com.toolrent.entities.LoanEntity;
import com.toolrent.services.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/loans")
@Tag(name = "Loan Controller", description = "Endpoints para Préstamos")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Registrar un nuevo préstamo",
            description = "Registra un préstamo de una unidad disponible de un grupo de herramientas a un cliente.")
    @ApiResponse(responseCode = "200", description = "Préstamo registrado")
    @ApiResponse(responseCode = "400", description = "No hay unidades disponibles")
    public ResponseEntity<LoanEntity> registerLoan(
            @RequestParam Long toolGroupId,
            @RequestParam Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dueDate) {

        LoanEntity loan = loanService.registerLoan(toolGroupId, customerId, dueDate);
        return ResponseEntity.ok(loan);
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Registrar devolución de préstamo")
    public ResponseEntity<Void> returnLoan(@PathVariable Long id) {
        loanService.returnLoan(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ResponseEntity<List<LoanActiveDTO>> getActiveLoans(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(loanService.getActiveLoans(from, to));
    }
}