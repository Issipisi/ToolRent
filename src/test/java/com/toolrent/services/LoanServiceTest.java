package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.CustomerRepository;
import com.toolrent.repositories.KardexMovementRepository;
import com.toolrent.repositories.LoanRepository;
import com.toolrent.repositories.ToolRepository;
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
    private ToolRepository toolRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private KardexMovementRepository kardexMovementRepository;

    @InjectMocks
    private LoanService loanService;

    /* ---------- REGISTRAR PRÉSTAMO ---------- */

    @Test
    void whenRegisterLoan_withValidData_thenSuccess() {
        // Given
        Long toolId = 1L;
        Long customerId = 10L;
        LocalDateTime dueDate = LocalDateTime.now().plusDays(5);

        ToolEntity tool = buildTool(toolId, "Taladro", "Eléctrica", 50000.0, 5000.0, 5, ToolStatus.AVAILABLE);
        CustomerEntity customer = buildCustomer(customerId, "Ana", "12.345.678-9", "+56912345678", "ana@test.com");

        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        LoanEntity result = loanService.registerLoan(toolId, customerId, dueDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTool()).isEqualTo(tool);
        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getDueDate()).isEqualTo(dueDate);
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.LOANED);
        assertThat(tool.getStock()).isEqualTo(4); // -1

        verify(toolRepository).save(tool);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    void whenRegisterLoan_withToolNotAvailable_thenThrows() {
        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 0, ToolStatus.LOANED);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));

        assertThatThrownBy(() -> loanService.registerLoan(1L, 10L, LocalDateTime.now().plusDays(2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tool not available for loan");

        verifyNoInteractions(loanRepository, kardexMovementRepository);
    }

    @Test
    void whenRegisterLoan_withCustomerNotFound_thenThrows() {
        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 1, ToolStatus.AVAILABLE);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.registerLoan(1L, 99L, LocalDateTime.now().plusDays(2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");

        verifyNoInteractions(loanRepository, kardexMovementRepository);
    }

    @Test
    void whenRegisterLoan_withToolNotFound_thenThrows() {
        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.registerLoan(999L, 10L, LocalDateTime.now().plusDays(2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tool not found");

        verifyNoInteractions(loanRepository, kardexMovementRepository);
    }

    @Test
    void whenRegisterLoan_withDueDateBeforeNow_thenCalculatesOneDay() {
        // DueDate en el pasado → days ≤ 0 → devuelve 1 día
        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 1,
                ToolStatus.AVAILABLE);
        CustomerEntity customer = buildCustomer(10L, "Ana", "11.111.111-1", "+56911111111",
                "ana@test.com");
        LocalDateTime dueDate = LocalDateTime.now().minusDays(2); // ← antes de hoy

        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(i -> i.getArgument(0));

        LoanEntity result = loanService.registerLoan(1L, 10L, dueDate);

        assertThat(result.getTotalCost()).isEqualTo(10.0); // 1 día × 10.0
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    /* ---------- DEVOLUCIÓN ---------- */

    @Test
    void whenReturnLoan_onTime_thenSuccess() {
        // Given
        Long loanId = 1L;
        LocalDateTime dueDate = LocalDateTime.now().plusDays(5);
        LocalDateTime returnDate = dueDate.minusDays(1); // antes

        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 0, ToolStatus.LOANED);
        CustomerEntity customer = buildCustomer(10L, "Luis", "22.222.222-2", "+56987654321", "luis@test.com");
        LoanEntity loan = buildLoan(loanId, customer, tool, dueDate);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        // When
        loanService.returnLoan(loanId);

        // Then
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
        assertThat(tool.getStock()).isEqualTo(1); // +1
        assertThat(loan.getReturnDate()).isNotNull();
        assertThat(loan.getFineAmount()).isZero(); // no hay retraso

        verify(toolRepository).save(tool);
        verify(loanRepository).save(loan);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class)); // RETURN
    }

    @Test
    void whenReturnLoan_withDelay_thenAppliesFine() {
        // Given
        Long loanId = 1L;
        LocalDateTime dueDate = LocalDateTime.now().minusDays(3); // vencido
        LocalDateTime returnDate = LocalDateTime.now(); // hoy

        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 0, ToolStatus.LOANED);
        CustomerEntity customer = buildCustomer(10L, "Ana", "11.111.111-1", "+56911111111", "ana@test.com");
        LoanEntity loan = buildLoan(loanId, customer, tool, dueDate);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        // When
        loanService.returnLoan(loanId);

        // Then
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
        assertThat(tool.getStock()).isEqualTo(1);
        assertThat(loan.getReturnDate()).isNotNull();
        assertThat(loan.getFineAmount()).isEqualTo(3000.0); // 3 días × 1000

        verify(toolRepository).save(tool);
        verify(loanRepository).save(loan);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    void whenReturnLoan_withLoanNotFound_thenThrows() {
        Long loanId = 999L;
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnLoan(loanId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Loan not found");

        verifyNoInteractions(toolRepository, kardexMovementRepository);
    }

    /* ---------- HELPERS ---------- */
    private ToolEntity buildTool(Long id, String name, String category,
                                 Double replacementValue, Double pricePerDay,
                                 int stock, ToolStatus status) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName(name);
        t.setCategory(category);
        t.setReplacementValue(replacementValue);
        t.setPricePerDay(pricePerDay);
        t.setStock(stock);
        t.setStatus(status);
        return t;
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

    private LoanEntity buildLoan(Long id, CustomerEntity customer, ToolEntity tool,
                                 LocalDateTime dueDate) {
        LoanEntity l = new LoanEntity();
        l.setId(id);
        l.setCustomer(customer);
        l.setTool(tool);
        l.setDueDate(dueDate);
        l.setReturnDate(null);
        l.setTotalCost(0.0);
        l.setFineAmount(0.0);
        return l;
    }
}