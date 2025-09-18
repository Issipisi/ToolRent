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

    /* ---------------------------------------------------------- */
    /* --------------------- REGISTRAR PRÉSTAMO ----------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Préstamo ok: herramienta y cliente existentes, dueDate futuro, stock disminuye 1 y permanece AVAILABLE")
    void whenRegisterLoan_withValidData_thenSuccess() {
        Long toolId = 1L;
        Long customerId = 10L;
        LocalDateTime dueDate = LocalDateTime.now().plusDays(5);

        ToolEntity tool = buildTool(toolId, "Taladro", "Eléctrica", 50000.0, 5000.0, 5, ToolStatus.AVAILABLE);
        CustomerEntity customer = buildCustomer(customerId, "Ana", "12.345.678-9", "+56912345678", "ana@test.com");

        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(i -> i.getArgument(0));

        LoanEntity result = loanService.registerLoan(toolId, customerId, dueDate);

        assertThat(result).isNotNull();
        assertThat(result.getTool()).isEqualTo(tool);
        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getDueDate()).isEqualTo(dueDate);
        assertThat(tool.getStock()).isEqualTo(4);
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.AVAILABLE);

        verify(toolRepository).save(tool);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    @DisplayName("Última unidad en stock: al prestarla el estado pasa a LOANED")
    void whenRegisterLoan_andLastUnit_thenToolStatusIsLoaned() {
        Long toolId = 1L;
        ToolEntity tool = buildTool(toolId, "A", "B", 100.0, 10.0, 1, ToolStatus.AVAILABLE);
        CustomerEntity customer = buildCustomer(10L, "Luis", "11.111.111-1", "+56911111111", "luis@test.com");

        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(i -> i.getArgument(0));

        loanService.registerLoan(toolId, 10L, LocalDateTime.now().plusDays(2));

        assertThat(tool.getStock()).isZero();
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.LOANED);
    }

    @Test
    @DisplayName("Herramienta no disponible (stock 0 o estado LOANED) → excepción")
    void whenRegisterLoan_withToolNotAvailable_thenThrows() {
        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 0, ToolStatus.LOANED);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));

        assertThatThrownBy(() -> loanService.registerLoan(1L, 10L, LocalDateTime.now().plusDays(2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tool not available for loan");

        verifyNoInteractions(loanRepository, kardexMovementRepository);
    }

    @Test
    @DisplayName("Cliente inexistente → excepción sin tocar base")
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
    @DisplayName("Herramienta inexistente → excepción")
    void whenRegisterLoan_withToolNotFound_thenThrows() {
        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.registerLoan(999L, 10L, LocalDateTime.now().plusDays(2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tool not found");

        verifyNoInteractions(loanRepository, kardexMovementRepository);
    }

    @Test
    @DisplayName("DueDate en el pasado: se cobra 1 día (mínimo forzado)")
    void whenRegisterLoan_withDueDateBeforeNow_thenCalculatesOneDay() {
        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 1, ToolStatus.AVAILABLE);
        CustomerEntity customer = buildCustomer(10L, "Ana", "11.111.111-1", "+56911111111", "ana@test.com");
        LocalDateTime dueDate = LocalDateTime.now().minusDays(2);

        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(i -> i.getArgument(0));

        LoanEntity result = loanService.registerLoan(1L, 10L, dueDate);

        assertThat(result.getTotalCost()).isEqualTo(10.0);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    /* ---------------------------------------------------------- */
    /* --------------------- DEVOLUCIÓN ------------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Devolución puntual: stock +1, sin multa, estado AVAILABLE")
    void whenReturnLoan_onTime_thenSuccess() {
        Long loanId = 1L;
        LocalDateTime dueDate = LocalDateTime.now().plusDays(5);

        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 0, ToolStatus.LOANED);
        CustomerEntity customer = buildCustomer(10L, "Luis", "22.222.222-2", "+56987654321", "luis@test.com");
        LoanEntity loan = buildLoan(loanId, customer, tool, dueDate);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        loanService.returnLoan(loanId);

        assertThat(tool.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
        assertThat(tool.getStock()).isEqualTo(1);
        assertThat(loan.getReturnDate()).isNotNull();
        assertThat(loan.getFineAmount()).isZero();

        verify(toolRepository).save(tool);
        verify(loanRepository).save(loan);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    @DisplayName("Devolución atrasada: calcula multa 3 días × 1000 y actualiza stock")
    void whenReturnLoan_withDelay_thenAppliesFine() {
        Long loanId = 1L;
        LocalDateTime dueDate = LocalDateTime.now().minusDays(3);

        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 0, ToolStatus.LOANED);
        CustomerEntity customer = buildCustomer(10L, "Ana", "11.111.111-1", "+56911111111", "ana@test.com");
        LoanEntity loan = buildLoan(loanId, customer, tool, dueDate);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        loanService.returnLoan(loanId);

        assertThat(tool.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
        assertThat(tool.getStock()).isEqualTo(1);
        assertThat(loan.getReturnDate()).isNotNull();
        assertThat(loan.getFineAmount()).isEqualTo(3000.0);

        verify(toolRepository).save(tool);
        verify(loanRepository).save(loan);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    @DisplayName("Préstamo inexistente → excepción")
    void whenReturnLoan_withLoanNotFound_thenThrows() {
        Long loanId = 999L;
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnLoan(loanId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Loan not found");

        verifyNoInteractions(toolRepository, kardexMovementRepository);
    }

    /* ---------------------------------------------------------- */
    /* --------------------- HELPERS ---------------------------- */
    /* ---------------------------------------------------------- */

    private ToolEntity buildTool(Long id, String name, String category,
                                 Double replacementValue, Double pricePerDay,
                                 Integer stock, ToolStatus status) {
        ToolEntity tool = new ToolEntity();
        tool.setId(id);
        tool.setName(name);
        tool.setCategory(category);
        tool.setReplacementValue(replacementValue);
        tool.setPricePerDay(pricePerDay);
        tool.setStock(stock);
        tool.setStatus(status);
        TariffEntity tariff = buildTariff(pricePerDay, pricePerDay * 100);
        tool.setTariff(tariff);
        return tool;
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

    private LoanEntity buildLoan(Long id, CustomerEntity customer, ToolEntity tool, LocalDateTime dueDate) {
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

    private TariffEntity buildTariff(Double dailyRentalRate, Double dailyFineRate) {
        TariffEntity t = new TariffEntity();
        t.setDailyRentalRate(dailyRentalRate);
        t.setDailyFineRate(dailyFineRate);
        return t;
    }
}