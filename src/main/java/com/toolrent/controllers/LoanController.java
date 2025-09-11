package com.toolrent.controllers;

import com.toolrent.entities.LoanEntity;
import com.toolrent.services.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
    @Operation(summary = "Registrar un nuevo préstamo", description = "Registra un préstamo de herramienta a cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Préstamo registrado"),
            @ApiResponse(responseCode = "400", description = "Herramienta no disponible"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<LoanEntity> registerLoan(@RequestParam Long toolId,
                                                   @RequestParam Long customerId,
                                                   @RequestParam LocalDateTime dueDate) {
        LoanEntity loan = loanService.registerLoan(toolId, customerId, dueDate);
        return ResponseEntity.ok(loan);
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Registrar devolución de préstamo", description = "Registra la devolución de una herramienta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devolución registrada"),
            @ApiResponse(responseCode = "404", description = "Préstamo no encontrado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<Void> returnLoan(@PathVariable Long id) {
        loanService.returnLoan(id);
        return ResponseEntity.ok().build();
    }
}