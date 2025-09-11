package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.CustomerRepository;
import com.toolrent.repositories.LoanRepository;
import com.toolrent.repositories.KardexMovementRepository;
import com.toolrent.repositories.ToolRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final ToolRepository toolRepository;
    private final KardexMovementRepository kardexMovementRepository;
    private final CustomerRepository customerRepository; // ← agregado

    public LoanService(LoanRepository loanRepository,
                       ToolRepository toolRepository,
                       KardexMovementRepository kardexMovementRepository,
                       CustomerRepository customerRepository) {
        this.loanRepository = loanRepository;
        this.toolRepository = toolRepository;
        this.kardexMovementRepository = kardexMovementRepository;
        this.customerRepository = customerRepository;
    }

    //Registrar préstamo
    public LoanEntity registerLoan(Long toolId, Long customerId, LocalDateTime dueDate) {
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        if (tool.getStatus() != ToolStatus.AVAILABLE || tool.getStock() <= 0) {
            throw new RuntimeException("Tool not available for loan");
        }

        // Busca cliente real
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        LoanEntity loan = new LoanEntity();
        loan.setCustomer(customer);   // ← cliente persistente
        loan.setTool(tool);
        loan.setDueDate(dueDate);
        loan.setTotalCost(calculateTotalCost(tool, dueDate));

        tool.setStatus(ToolStatus.LOANED);
        tool.setStock(tool.getStock() - 1);
        toolRepository.save(tool);

        LoanEntity savedLoan = loanRepository.save(loan);

        // Kardex (opcional)
        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setTool(tool);
        movement.setCustomer(customer); // ← cliente real
        movement.setMovementType(MovementType.LOAN);
        movement.setDetails("Préstamo a cliente ID: " + customerId);
        kardexMovementRepository.save(movement);

        return savedLoan;
    }

    //Registrar Devolución
    public void returnLoan(Long loanId) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        ToolEntity tool = loan.getTool();
        CustomerEntity customer = loan.getCustomer(); // ← Obtener el cliente del préstamo

        tool.setStatus(ToolStatus.AVAILABLE);
        tool.setStock(tool.getStock() + 1);
        loan.setReturnDate(LocalDateTime.now());

        if (loan.getReturnDate().isAfter(loan.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), loan.getReturnDate());
            loan.setFineAmount(daysLate * 1000.0);
        }

        toolRepository.save(tool);
        loanRepository.save(loan);

        // Generar movimiento en Kardex - USAR EL CLIENTE REAL
        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setTool(tool);
        movement.setCustomer(customer); // ← Usar el cliente persistente del préstamo
        movement.setMovementType(MovementType.RETURN);
        movement.setDetails("Devolución de préstamo ID: " + loanId);

        kardexMovementRepository.save(movement);
    }

    private Double calculateTotalCost(ToolEntity tool, LocalDateTime dueDate) {
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
        return tool.getPricePerDay() * (days > 0 ? days : 1);
    }
}