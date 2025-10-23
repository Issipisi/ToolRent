package com.toolrent.controllers;

import com.toolrent.dto.LoanActiveDTO;
import com.toolrent.entities.LoanEntity;
import com.toolrent.services.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
    @Operation(summary = "Registrar un nuevo préstamo", description = "Añade un nuevo préstamo")
    @ApiResponse(responseCode = "200", description = "Préstamo registrado")
    @ApiResponse(responseCode = "400", description = "No hay unidades disponibles")
    public ResponseEntity<LoanEntity> registerLoan(
            @RequestParam Long toolGroupId,
            @RequestParam Long customerId,
            @RequestParam String dueDate) {

        LocalDateTime due = LocalDateTime.parse(dueDate.replace(" ", "T"));
        LoanEntity loan = loanService.registerLoan(toolGroupId, customerId, due);
        return ResponseEntity.ok(loan);
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Registrar devolución de préstamo", description = "Devolución de una herramienta")
    public ResponseEntity<String> returnLoan(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0.0") Double damageCharge,
            @RequestParam(required = false, defaultValue = "false") Boolean irreparable) {

        loanService.returnLoan(id, damageCharge, irreparable);
        return ResponseEntity.ok("Devolución registrada");
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Muestra los préstamos activos", description = "Muestra todos los Loans en estado LOAN")
    public ResponseEntity<List<LoanActiveDTO>> getActiveLoans() {
        return ResponseEntity.ok(loanService.getActiveLoans());
    }

    @PutMapping("/{id}/pay-debts")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Pagar deudas del préstamo", description = "Elimina todas las deudas por daño y deudas")
    public ResponseEntity<String> payDebts(@PathVariable Long id) {
        loanService.payDebts(id);
        return ResponseEntity.ok("Deudas pagadas");
    }

    @PutMapping("/{id}/damage")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aplicar cargo por daño a un préstamo", description = "Modifica el cargo por daño" +
            " de una herramienta devuelta")
    public ResponseEntity<Void> applyDamage(@PathVariable Long id,
                                            @RequestParam Double amount,
                                            @RequestParam(defaultValue = "false") boolean irreparable) {
        loanService.applyDamage(id, amount, irreparable);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending-payment")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Muestra préstamos con deudas", description = "Préstamos con pago pendiente")
    public ResponseEntity<List<LoanActiveDTO>> getPendingPayment() {
        return ResponseEntity.ok(loanService.getPendingPayment());
    }

    @GetMapping("/returned-with-debts")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Mostrar préstamos con deudas", description = "Préstamos devueltos que aún tienen " +
            "multas o daños sin pagar.")
    public ResponseEntity<List<LoanActiveDTO>> getReturnedWithDebts() {
        return ResponseEntity.ok(loanService.getReturnedWithDebts());
    }

    @ExceptionHandler(RuntimeException.class)
    @Operation(summary = "Mensajes de restricción", description = "Endpoint para que el Front indique el mensaje de restricción de Loan")
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}