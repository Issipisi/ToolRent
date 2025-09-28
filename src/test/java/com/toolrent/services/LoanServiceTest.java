package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private ToolGroupRepository toolGroupRepository;

    @Mock
    private ToolUnitRepository toolUnitRepository;

    @Mock
    private KardexMovementRepository kardexMovementRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private LoanService loanService;

    /* ---------------------------------------------------------- */
    /* --------------------- REGISTRAR PRÉSTAMO ----------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Préstamo ok: grupo y cliente existentes, dueDate futuro, unidad pasa a LOANED")
    void whenRegisterLoan_withValidData_thenSuccess() {
        Long toolGroupId = 1L;
        Long customerId = 10L;
        LocalDateTime dueDate = LocalDateTime.now().plusDays(5);

        ToolGroupEntity group = buildToolGroup(toolGroupId, "Taladro", "Eléctrica", 50000.0, 3500.0);
        ToolUnitEntity unit = group.getUnits().get(0);
        CustomerEntity customer = buildCustomer(customerId, "Ana", "12.345.678-9", "+56912345678", "ana@test.com");

        when(toolGroupRepository.findById(toolGroupId)).thenReturn(Optional.of(group));
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(toolGroupId, ToolStatus.AVAILABLE))
                .thenReturn(Optional.of(unit));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(i -> i.getArgument(0));

        LoanEntity result = loanService.registerLoan(toolGroupId, customerId, dueDate);

        assertThat(result).isNotNull();
        assertThat(result.getToolUnit()).isEqualTo(unit);
        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getDueDate()).isEqualTo(dueDate);
        assertThat(unit.getStatus()).isEqualTo(ToolStatus.LOANED);

        verify(toolUnitRepository).save(unit);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    @DisplayName("No hay unidades disponibles → excepción")
    void whenRegisterLoan_noAvailableUnit_thenThrows() {
        Long toolGroupId = 1L;
        ToolGroupEntity group = buildToolGroup(toolGroupId, "A", "B", 1000.0, 0);

        when(toolGroupRepository.findById(toolGroupId)).thenReturn(Optional.of(group));
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(toolGroupId, ToolStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.registerLoan(toolGroupId, 10L, LocalDateTime.now().plusDays(2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay unidades disponibles");

        verifyNoInteractions(loanRepository, kardexMovementRepository);
    }

    @Test
    @DisplayName("Cliente inexistente → excepción")
    void whenRegisterLoan_customerNotFound_thenThrows() {
        Long toolGroupId = 1L;
        ToolGroupEntity group = buildToolGroup(toolGroupId, "A", "B", 1000.0, 1);
        ToolUnitEntity unit = group.getUnits().get(0);

        when(toolGroupRepository.findById(toolGroupId)).thenReturn(Optional.of(group));
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(toolGroupId, ToolStatus.AVAILABLE))
                .thenReturn(Optional.of(unit));
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.registerLoan(toolGroupId, 99L, LocalDateTime.now().plusDays(2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");

        verifyNoInteractions(loanRepository, kardexMovementRepository);
    }

    @Test
    @DisplayName("DueDate en el pasado → calcula 1 día mínimo")
    void whenRegisterLoan_pastDueDate_thenCalculatesOneDay() {
        Long toolGroupId = 1L;
        ToolGroupEntity group = buildToolGroup(toolGroupId, "A", "B", 100.0, 100.0); // ✅ 100.0
        ToolUnitEntity unit = group.getUnits().get(0);
        CustomerEntity customer = buildCustomer(10L, "Ana", "11.111.111-1", "+56911111111", "ana@test.com");
        LocalDateTime dueDate = LocalDateTime.now().minusDays(2);

        when(toolGroupRepository.findById(toolGroupId)).thenReturn(Optional.of(group));
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(toolGroupId, ToolStatus.AVAILABLE))
                .thenReturn(Optional.of(unit));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(i -> i.getArgument(0));

        LoanEntity result = loanService.registerLoan(toolGroupId, 10L, dueDate);

        assertThat(result.getTotalCost()).isEqualTo(100.0); //  1 día × 100.0
    }

    /* ---------------------------------------------------------- */
    /* --------------------- DEVOLUCIÓN ------------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Devolución puntual: unidad pasa a AVAILABLE, sin multa")
    void whenReturnLoan_onTime_thenSuccess() {
        Long loanId = 1L;
        LocalDateTime dueDate = LocalDateTime.now().plusDays(5);

        ToolGroupEntity group = buildToolGroup(1L, "A", "B", 100.0, 1);
        ToolUnitEntity unit = group.getUnits().get(0);
        unit.setStatus(ToolStatus.LOANED);
        CustomerEntity customer = buildCustomer(10L, "Luis", "22.222.222-2", "+56987654321", "luis@test.com");
        LoanEntity loan = buildLoan(loanId, customer, unit, dueDate);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        loanService.returnLoan(loanId);

        assertThat(unit.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
        assertThat(loan.getReturnDate()).isNotNull();
        assertThat(loan.getFineAmount()).isZero();

        verify(toolUnitRepository).save(unit);
        verify(loanRepository).save(loan);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    @DisplayName("Devolución atrasada: calcula multa")
    void whenReturnLoan_withDelay_thenAppliesFine() {
        Long loanId = 1L;
        LocalDateTime dueDate = LocalDateTime.now().minusDays(3);

        ToolGroupEntity group = buildToolGroup(1L, "A", "B", 100.0, 1000.0);
        ToolUnitEntity unit = group.getUnits().get(0);
        unit.setStatus(ToolStatus.LOANED);
        CustomerEntity customer = buildCustomer(10L, "Ana", "11.111.111-1", "+56911111111",
                "ana@test.com");
        LoanEntity loan = buildLoan(loanId, customer, unit, dueDate);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        loanService.returnLoan(loanId);

        assertThat(unit.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
        assertThat(loan.getReturnDate()).isNotNull();
        assertThat(loan.getFineAmount()).isEqualTo(6000.0);

        verify(toolUnitRepository).save(unit);
        verify(loanRepository).save(loan);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    @DisplayName("Préstamo inexistente → excepción")
    void whenReturnLoan_loanNotFound_thenThrows() {
        when(loanRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnLoan(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Loan not found");

        verifyNoInteractions(toolUnitRepository, kardexMovementRepository);
    }

    /* ---------------------------------------------------------- */
    /* --------------------- HELPERS ---------------------------- */
    /* ---------------------------------------------------------- */

    private ToolGroupEntity buildToolGroup(Long id, String name, String category, double replacementValue, double dailyRate) {
        ToolGroupEntity group = new ToolGroupEntity();
        group.setId(id);
        group.setName(name);
        group.setCategory(category);
        group.setReplacementValue(replacementValue);

        TariffEntity tariff = new TariffEntity();
        tariff.setDailyRentalRate(dailyRate);
        tariff.setDailyFineRate(dailyRate * 2);
        group.setTariff(tariff);

        for (int i = 0; i < 3; i++) {
            ToolUnitEntity unit = new ToolUnitEntity();
            unit.setId((long) (i + 1));
            unit.setStatus(ToolStatus.AVAILABLE);
            unit.setToolGroup(group);
            group.getUnits().add(unit);
        }
        return group;
    }

    private CustomerEntity buildCustomer(Long id, String name, String rut, String phone, String email) {
        CustomerEntity c = new CustomerEntity();
        c.setId(id);
        c.setName(name);
        c.setRut(rut);
        c.setPhone(phone);
        c.setEmail(email);
        c.setStatus(CustomerStatus.ACTIVE);
        return c;
    }

    private LoanEntity buildLoan(Long id, CustomerEntity customer, ToolUnitEntity unit, LocalDateTime dueDate) {
        LoanEntity l = new LoanEntity();
        l.setId(id);
        l.setCustomer(customer);
        l.setToolUnit(unit);
        l.setDueDate(dueDate);
        l.setReturnDate(null);
        l.setTotalCost(0.0);
        l.setFineAmount(0.0);
        return l;
    }
}