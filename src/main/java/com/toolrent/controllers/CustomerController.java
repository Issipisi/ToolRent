package com.toolrent.controllers;

import com.toolrent.entities.CustomerEntity;
import com.toolrent.entities.CustomerStatus;
import com.toolrent.services.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/customers")
@Tag(name = "Customers Controller", description = "Endpoints para Clientes")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un nuevo cliente", description = "Registra un cliente con datos básicos " +
            "(nombre, RUT, teléfono, correo).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<CustomerEntity> registerCustomer(@RequestParam String name,
                                                           @RequestParam String rut,
                                                           @RequestParam String phone,
                                                           @RequestParam String email) {
        CustomerEntity customer = customerService.registerCustomer(name, rut, phone, email);
        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de cliente", description = "Cambia el estado a 'RESTRICTED' o 'ACTIVE'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado cambiado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<Void> changeStatus(@PathVariable Long id, @RequestParam CustomerStatus newStatus) {
        customerService.changeStatus(id, newStatus);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Listar todos los clientes", description = "Retorna todos los clientes para reportes o consulta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de clientes"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<Iterable<CustomerEntity>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Listar clientes activos")
    public ResponseEntity<List<CustomerEntity>> getActiveCustomers() {
        return ResponseEntity.ok(customerService.getCustomersByStatus(CustomerStatus.ACTIVE));
    }
}
