package com.toolrent.services;

import com.toolrent.dto.LoanActiveDTO;
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
        /* ---------- Validaciones de negocio ---------- */
        /* Restricción fecha de devolución
        if (dueDate.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("La fecha de devolución no puede ser anterior a la fecha actual");
        }*/

        // Préstamos vencidos sin devolver
        boolean hasOverdueLoans = loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(customerId, LocalDateTime.now());
        if (hasOverdueLoans) {
            throw new RuntimeException("El cliente tiene préstamos vencidos sin devolver");
        }

        // Multas impagas
        boolean hasUnpaidFines = loanRepository.existsByCustomerIdAndFineAmountGreaterThanAndReturnDateIsNotNull(customerId, 0.0);
        if (hasUnpaidFines) {
            throw new RuntimeException("El cliente tiene multas impagas");
        }

        // Deudas por reposición (préstamos con daño sin pagar)
        boolean hasUnpaidDamage = loanRepository.existsByCustomerIdAndDamageChargeGreaterThanAndReturnDateIsNotNull(customerId, 0.0);
        if (hasUnpaidDamage) {
            throw new RuntimeException("El cliente tiene cargos por daño sin pagar");
        }

        /* ---------- LÍMITES DE PRÉSTAMO ---------- */

        // Máximo 5 préstamos activos por cliente
        long activeLoans = loanRepository.countByCustomerIdAndReturnDateIsNull(customerId);
        if (activeLoans >= 5) {
            throw new RuntimeException("El cliente ya tiene 5 préstamos activos (máximo permitido)");
        }

        // No puede pedir la MISMA herramienta que ya tiene en préstamo
        boolean alreadyHasSameTool = loanRepository.existsByCustomerIdAndToolUnitToolGroupIdAndReturnDateIsNull(customerId, toolGroupId);
        if (alreadyHasSameTool) {
            throw new RuntimeException("El cliente ya tiene una unidad de esta herramienta en préstamo");
        }

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
    public void returnLoan(Long loanId, Double damageCharge, boolean irreparable) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        ToolUnitEntity unit = loan.getToolUnit();
        CustomerEntity customer = loan.getCustomer();

        /* ---------- Multa por atraso ---------- */
        loan.setReturnDate(LocalDateTime.now());
        if (loan.getReturnDate().isAfter(loan.getDueDate())) {
            long lateDays = ChronoUnit.DAYS.between(loan.getDueDate(), loan.getReturnDate());
            double dailyFine = unit.getToolGroup().getTariff().getDailyFineRate();
            loan.setFineAmount(lateDays * dailyFine);
        }

        /* ---------- Daños ---------- */
        if (irreparable) {
            loan.setDamageCharge(unit.getToolGroup().getReplacementValue()); // valor fijo reposición
            unit.setStatus(ToolStatus.RETIRED); // damos de baja la unidad
            toolUnitRepository.save(unit);
        } else {
            loan.setDamageCharge(damageCharge); // daño leve
        }

        unit.setStatus(ToolStatus.AVAILABLE);
        toolUnitRepository.save(unit);
        loanRepository.save(loan);

        /* ---------- Kardex ---------- */
        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setToolUnit(unit);
        movement.setCustomer(customer);
        movement.setMovementType(irreparable ? MovementType.RETIRE : MovementType.RETURN);
        movement.setDetails("Devolución ID: " + loanId + " - Daño: " + damageCharge + " - Usuario: "
                + SecurityConfig.getCurrentUsername());
        kardexMovementRepository.save(movement);
    }

    // CÁLCULO DE COSTO
    private Double calculateTotalCost(ToolGroupEntity toolGroup, LocalDateTime dueDate) {
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
        days = Math.max(1, days);
        return toolGroup.getTariff().getDailyRentalRate() * days;
    }

    // Aplicar cargo por daño
    public void applyDamage(Long loanId, Double amount, boolean irreparable) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getReturnDate() == null) {
            throw new RuntimeException("Solo se puede aplicar daño a préstamos devueltos");
        }

        if (irreparable) {
            loan.setDamageCharge(loan.getToolUnit().getToolGroup().getReplacementValue());
            loan.getToolUnit().setStatus(ToolStatus.RETIRED);
            toolUnitRepository.save(loan.getToolUnit());
        } else {
            loan.setDamageCharge(amount);
        }

        loanRepository.save(loan);
    }

    // Pagar deudas
    public void payDebts(Long loanId) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getReturnDate() == null) {
            throw new RuntimeException("Solo se pueden pagar deudas de préstamos devueltos");
        }

        loan.setFineAmount(0.0);
        loan.setDamageCharge(0.0);
        loanRepository.save(loan);
    }

    // Obtener préstamos activos
        public List<LoanActiveDTO> getActiveLoans() {
        return loanRepository.findActiveLoansInRange(
                LocalDateTime.now().minusMonths(1),
                LocalDateTime.now().plusMonths(1)
        );
    }

    // Obtener préstamos con deudas
    public List<LoanActiveDTO> getReturnedWithDebts() {
        return loanRepository.findReturnedWithDebts();
    }

    // Obtener préstamos pendientes de pago
    public List<LoanActiveDTO> getPendingPayment() {
        return loanRepository.findPendingPayment();
    }

}