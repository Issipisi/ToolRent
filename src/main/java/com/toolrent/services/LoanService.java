package com.toolrent.services;

import com.toolrent.config.LoanActiveDTO;
import com.toolrent.config.SecurityConfig;
import com.toolrent.entities.*;
import com.toolrent.repositories.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final ToolGroupRepository toolGroupRepository;
    private final ToolUnitRepository toolUnitRepository;
    private final KardexMovementRepository kardexMovementRepository;
    private final CustomerRepository customerRepository;


    public LoanService(LoanRepository loanRepository,
                       ToolGroupRepository toolGroupRepository,
                       ToolUnitRepository toolUnitRepository,
                       KardexMovementRepository kardexMovementRepository,
                       CustomerRepository customerRepository) {
        this.loanRepository = loanRepository;
        this.toolGroupRepository = toolGroupRepository;
        this.toolUnitRepository = toolUnitRepository;
        this.kardexMovementRepository = kardexMovementRepository;
        this.customerRepository = customerRepository;
    }

    // REGISTRAR PRÉSTAMO
    public LoanEntity registerLoan(Long toolGroupId, Long customerId, LocalDateTime dueDate) {
        ToolGroupEntity toolGroup = toolGroupRepository.findById(toolGroupId)
                .orElseThrow(() -> new RuntimeException("Grupo de herramientas no encontrado"));

        ToolUnitEntity availableUnit = toolUnitRepository
                .findFirstByToolGroupIdAndStatus(toolGroupId, ToolStatus.AVAILABLE)
                .orElseThrow(() -> new RuntimeException("No hay unidades disponibles"));

        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        LoanEntity loan = new LoanEntity();
        loan.setCustomer(customer);
        loan.setToolUnit(availableUnit);
        loan.setDueDate(dueDate);
        loan.setTotalCost(calculateTotalCost(toolGroup, dueDate));

        availableUnit.setStatus(ToolStatus.LOANED);
        toolUnitRepository.save(availableUnit);

        LoanEntity savedLoan = loanRepository.save(loan);

        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setToolUnit(availableUnit);
        movement.setCustomer(customer);
        movement.setMovementType(MovementType.LOAN);
        movement.setDetails("Préstamo a cliente ID: " + customerId + " - Usuario: " + SecurityConfig.getCurrentUsername());
        kardexMovementRepository.save(movement);

        return savedLoan;
    }

    // REGISTRAR DEVOLUCIÓN
    public void returnLoan(Long loanId) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        ToolUnitEntity unit = loan.getToolUnit();
        CustomerEntity customer = loan.getCustomer();

        unit.setStatus(ToolStatus.AVAILABLE);
        loan.setReturnDate(LocalDateTime.now());

        if (loan.getReturnDate().isAfter(loan.getDueDate())) {
            long lateDays = ChronoUnit.DAYS.between(loan.getDueDate(), loan.getReturnDate());
            double dailyFine = unit.getToolGroup().getTariff().getDailyFineRate();
            loan.setFineAmount(lateDays * dailyFine);
        }

        toolUnitRepository.save(unit);
        loanRepository.save(loan);

        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setToolUnit(unit);
        movement.setCustomer(customer);
        movement.setMovementType(MovementType.RETURN);
        movement.setDetails("Devolución de préstamo ID: " + loanId + " - Usuario: " + SecurityConfig.getCurrentUsername());
        kardexMovementRepository.save(movement);
    }

    // CÁLCULO DE COSTO
    private Double calculateTotalCost(ToolGroupEntity toolGroup, LocalDateTime dueDate) {
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
        days = Math.max(1, days);
        return toolGroup.getTariff().getDailyRentalRate() * days;
    }

    public List<LoanActiveDTO> getActiveLoans(LocalDateTime from, LocalDateTime to) {
        return loanRepository.findActiveLoansInRange(from, to);
    }
}