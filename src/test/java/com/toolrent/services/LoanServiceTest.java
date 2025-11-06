package com.toolrent.services;

import com.toolrent.config.SecurityConfig;
import com.toolrent.dto.LoanActiveDTO;
import com.toolrent.entities.*;
import com.toolrent.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.toolrent.entities.CustomerStatus.ACTIVE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private ToolGroupRepository toolGroupRepository;
    @Mock private ToolUnitRepository toolUnitRepository;
    @Mock private KardexMovementRepository kardexMovementRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private LoanService loanService;

    /* ======================================================================
              1. registerLoan
       ====================================================================== */

    /* -------------- fecha inválida (nueva) -------------- */
    @Test @DisplayName("registerLoan – dueDate anterior a ahora → excepción")
    void registerLoan_pastDueDate(){
        Long groupId = 1L, customerId = 10L;
        LocalDateTime past = LocalDateTime.now().minusMinutes(5);

        /* ningún otro stub necesario: la validación falla antes */
        assertThatThrownBy(() -> loanService.registerLoan(groupId, customerId, past))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fecha de devolución no puede ser anterior a la fecha actual");
    }

    @Test @DisplayName("registerLoan – dueDate == now (límite) → permite")
    void registerLoan_dueDateNow(){
        try (MockedStatic<SecurityConfig> mocked = mockStatic(SecurityConfig.class)){
            mocked.when(SecurityConfig::getCurrentUsername).thenReturn("emp1");
            Long groupId = 1L, customerId = 10L;
            LocalDateTime due = LocalDateTime.now().plusSeconds(1); //  siempre futuro

            CustomerEntity customer = buildCustomer(customerId);
            ToolGroupEntity group = buildToolGroup(1L, 15000.0);
            ToolUnitEntity unit   = buildUnit(5L, group, ToolStatus.AVAILABLE);

            when(loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(eq(customerId), any())).thenReturn(false);
            when(loanRepository.existsByCustomerIdAndFineAmountGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
            when(loanRepository.existsByCustomerIdAndDamageChargeGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
            when(loanRepository.countByCustomerIdAndReturnDateIsNull(customerId)).thenReturn(0L);
            when(loanRepository.existsByCustomerIdAndToolUnitToolGroupIdAndReturnDateIsNull(customerId, groupId)).thenReturn(false);
            when(toolGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(toolUnitRepository.findFirstByToolGroupIdAndStatus(groupId, ToolStatus.AVAILABLE)).thenReturn(Optional.of(unit));
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(i -> i.getArgument(0));

            LoanEntity res = loanService.registerLoan(groupId, customerId, due);

            assertThat(res).isNotNull();
            verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
        }
    }

    /* -------------- resto de restricciones -------------- */
    @Test @DisplayName("registerLoan – camino feliz")
    void registerLoan_ok(){
        try (MockedStatic<SecurityConfig> mocked = mockStatic(SecurityConfig.class)){
            mocked.when(SecurityConfig::getCurrentUsername).thenReturn("emp1");

            Long groupId = 1L, customerId = 10L;
            LocalDateTime due = LocalDateTime.now().plusDays(3);

            CustomerEntity customer = buildCustomer(customerId);
            ToolGroupEntity group = buildToolGroup(1L, 15000.0);
            ToolUnitEntity unit = buildUnit(5L, group, ToolStatus.AVAILABLE);

            /* --- mocks de validaciones (todas false) --- */
            when(loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(eq(customerId), any())).thenReturn(false);
            when(loanRepository.existsByCustomerIdAndFineAmountGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
            when(loanRepository.existsByCustomerIdAndDamageChargeGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
            when(loanRepository.countByCustomerIdAndReturnDateIsNull(customerId)).thenReturn(0L);
            when(loanRepository.existsByCustomerIdAndToolUnitToolGroupIdAndReturnDateIsNull(customerId, groupId)).thenReturn(false);

            when(toolGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(toolUnitRepository.findFirstByToolGroupIdAndStatus(groupId, ToolStatus.AVAILABLE)).thenReturn(Optional.of(unit));
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            LoanEntity saved = new LoanEntity();
            saved.setId(100L);
            when(loanRepository.save(any(LoanEntity.class))).thenReturn(saved);

            LoanEntity res = loanService.registerLoan(groupId, customerId, due);

            assertThat(res.getId()).isEqualTo(100L);
            assertThat(unit.getStatus()).isEqualTo(ToolStatus.LOANED);
            verify(toolUnitRepository).save(unit);
            verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
        }
    }

    @Test @DisplayName("registerLoan – cliente con préstamos vencidos")
    void registerLoan_overdue(){
        Long customerId = 1L;
        when(loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(eq(customerId), any())).thenReturn(true);

        assertThatThrownBy(() -> loanService.registerLoan(1L, customerId, LocalDateTime.now().plusDays(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("vencidos sin devolver");
    }

    @Test @DisplayName("registerLoan – multas impagas")
    void registerLoan_unpaidFine(){
        Long customerId = 1L;
        when(loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(eq(customerId), any())).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndFineAmountGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(true);

        assertThatThrownBy(() -> loanService.registerLoan(1L, customerId, LocalDateTime.now().plusDays(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("multas impagas");
    }

    @Test @DisplayName("registerLoan – daños sin pagar")
    void registerLoan_unpaidDamage(){
        Long customerId = 1L;
        when(loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(eq(customerId), any())).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndFineAmountGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndDamageChargeGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(true);

        assertThatThrownBy(() -> loanService.registerLoan(1L, customerId, LocalDateTime.now().plusDays(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("daño sin pagar");
    }

    @Test @DisplayName("registerLoan – máximo 5 préstamos")
    void registerLoan_maxActive(){
        Long customerId = 1L;
        when(loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(eq(customerId), any())).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndFineAmountGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndDamageChargeGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
        when(loanRepository.countByCustomerIdAndReturnDateIsNull(customerId)).thenReturn(5L);

        assertThatThrownBy(() -> loanService.registerLoan(1L, customerId, LocalDateTime.now().plusDays(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("5 préstamos activos");
    }

    @Test @DisplayName("registerLoan – ya tiene la misma herramienta")
    void registerLoan_sameTool(){
        Long customerId = 1L, groupId = 1L;
        when(loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(eq(customerId), any())).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndFineAmountGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndDamageChargeGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
        when(loanRepository.countByCustomerIdAndReturnDateIsNull(customerId)).thenReturn(1L);
        when(loanRepository.existsByCustomerIdAndToolUnitToolGroupIdAndReturnDateIsNull(customerId, groupId)).thenReturn(true);

        assertThatThrownBy(() -> loanService.registerLoan(groupId, customerId, LocalDateTime.now().plusDays(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya tiene una unidad de esta herramienta");
    }

    @Test @DisplayName("registerLoan – grupo no existe")
    void registerLoan_groupNotFound(){
        Long groupId = 99L, customerId = 1L;
        when(loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(eq(customerId), any())).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndFineAmountGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndDamageChargeGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
        when(loanRepository.countByCustomerIdAndReturnDateIsNull(customerId)).thenReturn(0L);
        when(loanRepository.existsByCustomerIdAndToolUnitToolGroupIdAndReturnDateIsNull(customerId, groupId)).thenReturn(false);
        when(toolGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.registerLoan(groupId, customerId, LocalDateTime.now().plusDays(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Grupo de herramientas no encontrado");
    }

    @Test @DisplayName("registerLoan – sin unidades disponibles")
    void registerLoan_noAvailableUnit(){
        Long groupId = 1L, customerId = 1L;
        ToolGroupEntity group = buildToolGroup(groupId, 15000.0);
        when(loanRepository.existsByCustomerIdAndReturnDateIsNullAndDueDateBefore(eq(customerId), any())).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndFineAmountGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
        when(loanRepository.existsByCustomerIdAndDamageChargeGreaterThanAndReturnDateIsNotNull(eq(customerId), eq(0.0))).thenReturn(false);
        when(loanRepository.countByCustomerIdAndReturnDateIsNull(customerId)).thenReturn(0L);
        when(loanRepository.existsByCustomerIdAndToolUnitToolGroupIdAndReturnDateIsNull(customerId, groupId)).thenReturn(false);
        when(toolGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(groupId, ToolStatus.AVAILABLE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.registerLoan(groupId, customerId, LocalDateTime.now().plusDays(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay unidades disponibles");
    }

    /* ======================================================================
          2. returnLoan
       ====================================================================== */

    @Test @DisplayName("returnLoan – sin atraso, sin daño")
    void returnLoanNoLateNoDamage(){
        try (MockedStatic<SecurityConfig> mocked = mockStatic(SecurityConfig.class)){
            mocked.when(SecurityConfig::getCurrentUsername).thenReturn("emp1");
            LocalDateTime now = LocalDateTime.now();
            ToolGroupEntity group = buildToolGroup(1L, 15000.0);
            ToolUnitEntity unit = buildUnit(10L, group, ToolStatus.LOANED);
            CustomerEntity customer = buildCustomer(1L);
            /* se entregó hoy y se devuelve hoy → sin multa */
            LoanEntity loan = buildLoan(50L, customer, unit, now.plusHours(1), null);

            when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any())).thenReturn(loan);
            when(toolUnitRepository.save(any())).thenReturn(unit);

            loanService.returnLoan(50L, 0.0, false);

            assertThat(loan.getFineAmount()).isZero();
            assertThat(unit.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
            verify(kardexMovementRepository).save(argThat(m -> m.getMovementType() == MovementType.RETURN));
        }
    }

    @Test @DisplayName("returnLoan – con atraso")
    void returnLoanLate(){
        LocalDateTime now = LocalDateTime.now();
        ToolGroupEntity group = buildToolGroup(1L, 15000.0);
        ToolUnitEntity unit = buildUnit(10L, group, ToolStatus.LOANED);
        CustomerEntity customer = buildCustomer(1L);
        /* venció hace 2 días */
        LoanEntity loan = buildLoan(50L, customer, unit, now.minusDays(2), null);

        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenReturn(loan);
        when(toolUnitRepository.save(any())).thenReturn(unit);

        loanService.returnLoan(50L, 0.0, false);

        assertThat(loan.getFineAmount()).isEqualTo(2 * 500.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {100.0, 250.0})
    @DisplayName("returnLoan – daño leve")
    void returnLoan_damageLight(double amount){
        LocalDateTime now = LocalDateTime.now();
        ToolGroupEntity group = buildToolGroup(1L, 15000.0);
        ToolUnitEntity unit = buildUnit(10L, group, ToolStatus.LOANED);
        CustomerEntity customer = buildCustomer(1L);
        LoanEntity loan = buildLoan(50L, customer, unit, now.minusDays(1), null);

        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenReturn(loan);
        when(toolUnitRepository.save(any())).thenReturn(unit);

        loanService.returnLoan(50L, amount, false);

        assertThat(loan.getDamageCharge()).isEqualTo(amount);
        assertThat(unit.getStatus()).isEqualTo(ToolStatus.IN_REPAIR);
        verify(kardexMovementRepository).save(argThat(m -> m.getMovementType() == MovementType.REPAIR));
    }

    @Test @DisplayName("returnLoan – daño irreparable")
    void returnLoan_irreparable(){
        try (MockedStatic<SecurityConfig> mocked = mockStatic(SecurityConfig.class)){
            mocked.when(SecurityConfig::getCurrentUsername).thenReturn("emp1");
            LocalDateTime now = LocalDateTime.now();
            ToolGroupEntity group = buildToolGroup(1L, 15000.0);
            ToolUnitEntity unit = buildUnit(10L, group, ToolStatus.LOANED);
            CustomerEntity customer = buildCustomer(1L);
            LoanEntity loan = buildLoan(50L, customer, unit, now.minusDays(1), null);

            when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any())).thenReturn(loan);
            when(toolUnitRepository.save(any())).thenReturn(unit);

            loanService.returnLoan(50L, 0.0, true);

            assertThat(loan.getDamageCharge()).isEqualTo(15000.0);
            assertThat(unit.getStatus()).isEqualTo(ToolStatus.RETIRED);
            verify(kardexMovementRepository).save(argThat(m -> m.getMovementType() == MovementType.RETIRE));
        }
    }

    @Test @DisplayName("returnLoan – préstamo no existe")
    void returnLoan_notFound(){
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnLoan(99L, 0.0, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Loan not found");
    }

    /* ======================================================================
          3. applyDamage
       ====================================================================== */

    @Test @DisplayName("applyDamage – daño leve")
    void applyDamage_light(){
        LoanEntity loan = buildLoan(60L, null, null, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        loan.setDamageCharge(0.0);
        when(loanRepository.findById(60L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenReturn(loan);

        loanService.applyDamage(60L, 300.0, false);

        assertThat(loan.getDamageCharge()).isEqualTo(300.0);
    }

    @Test @DisplayName("applyDamage – irreparable")
    void applyDamage_irreparable(){
        ToolGroupEntity group = buildToolGroup(1L, 12000.0);
        ToolUnitEntity unit = buildUnit(20L, group, ToolStatus.AVAILABLE);
        LoanEntity loan = buildLoan(70L, null, unit, LocalDateTime.now().minusDays(1), LocalDateTime.now());

        when(loanRepository.findById(70L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenReturn(loan);
        when(toolUnitRepository.save(any())).thenReturn(unit);

        loanService.applyDamage(70L, 0.0, true);

        assertThat(loan.getDamageCharge()).isEqualTo(12000.0);
        assertThat(unit.getStatus()).isEqualTo(ToolStatus.RETIRED);
        verify(toolUnitRepository).save(unit);
    }

    @Test @DisplayName("applyDamage – préstamo no devuelto")
    void applyDamage_notReturned(){
        LoanEntity loan = buildLoan(80L, null, null, LocalDateTime.now().minusDays(1), null);
        when(loanRepository.findById(80L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.applyDamage(80L, 100.0, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Solo se puede aplicar daño a préstamos devueltos");
    }

    @Test @DisplayName("applyDamage – préstamo no existe")
    void applyDamage_notFound(){
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.applyDamage(99L, 100.0, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Loan not found");
    }

    /* ======================================================================
              4. payDebts
       ====================================================================== */

    @Test @DisplayName("payDebts – ok")
    void payDebts_ok(){
        LoanEntity loan = buildLoan(90L, null, null, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        loan.setFineAmount(200.0);
        loan.setDamageCharge(300.0);
        when(loanRepository.findById(90L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenReturn(loan);

        loanService.payDebts(90L);

        assertThat(loan.getFineAmount()).isZero();
        assertThat(loan.getDamageCharge()).isZero();
    }

    @Test @DisplayName("payDebts – préstamo no devuelto")
    void payDebts_notReturned(){
        LoanEntity loan = buildLoan(91L, null, null, LocalDateTime.now().minusDays(1), null);
        when(loanRepository.findById(91L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.payDebts(91L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Solo se pueden pagar deudas de préstamos devueltos");
    }

    @Test @DisplayName("payDebts – préstamo no existe")
    void payDebts_notFound(){
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.payDebts(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Loan not found");
    }

    /* ======================================================================
              5. queries
       ====================================================================== */

    @Test @DisplayName("getActiveLoans")
    void getActiveLoans(){
        List<LoanActiveDTO> list = List.of(mock(LoanActiveDTO.class));
        when(loanRepository.findActiveLoansInRange(any(), any())).thenReturn(list);

        List<LoanActiveDTO> res = loanService.getActiveLoans();

        assertThat(res).hasSize(1);
    }

    @Test @DisplayName("getReturnedWithDebts")
    void getReturnedWithDebts(){
        List<LoanActiveDTO> list = List.of(mock(LoanActiveDTO.class));
        when(loanRepository.findReturnedWithDebts()).thenReturn(list);

        List<LoanActiveDTO> res = loanService.getReturnedWithDebts();

        assertThat(res).hasSize(1);
    }

    @Test @DisplayName("getPendingPayment")
    void getPendingPayment(){
        List<LoanActiveDTO> list = List.of(mock(LoanActiveDTO.class));
        when(loanRepository.findPendingPayment()).thenReturn(list);

        List<LoanActiveDTO> res = loanService.getPendingPayment();

        assertThat(res).hasSize(1);
    }

    /* ======================================================================
                                        Helpers
       ====================================================================== */

    private CustomerEntity buildCustomer(Long id){
        CustomerEntity c = new CustomerEntity();
        c.setId(id);
        c.setStatus(ACTIVE);
        return c;
    }

    private ToolGroupEntity buildToolGroup(Long id, double replacement){
        ToolGroupEntity g = new ToolGroupEntity();
        g.setId(id);
        TariffEntity t = new TariffEntity();
        t.setDailyRentalRate(1000.0);
        t.setDailyFineRate(500.0);
        g.setTariff(t);
        g.setReplacementValue(replacement);
        return g;
    }

    private ToolUnitEntity buildUnit(Long id, ToolGroupEntity group, ToolStatus status){
        ToolUnitEntity u = new ToolUnitEntity();
        u.setId(id);
        u.setToolGroup(group);
        u.setStatus(status);
        return u;
    }

    private LoanEntity buildLoan(Long id, CustomerEntity customer, ToolUnitEntity unit,
                                 LocalDateTime dueDate, LocalDateTime returnDate){
        LoanEntity l = new LoanEntity();
        l.setId(id);
        l.setCustomer(customer);
        l.setToolUnit(unit);
        l.setDueDate(dueDate);
        l.setReturnDate(returnDate);
        return l;
    }
}