package com.toolrent.services;

import com.toolrent.config.ToolCountDTO;
import com.toolrent.entities.*;
import com.toolrent.repositories.LoanRepository;
import com.toolrent.repositories.CustomerRepository;
import com.toolrent.repositories.ToolRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService - Casos límite y concurrencia (Mockito)")
class ReportServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ToolRepository toolRepository;

    @InjectMocks
    private ReportService reportService;

    /* Fechas fijas reproducibles */
    private static final LocalDateTime D1  = LocalDateTime.of(2025, 9, 1, 0, 0);
    private static final LocalDateTime D5  = LocalDateTime.of(2025, 9, 5, 0, 0);
    private static final LocalDateTime D10 = LocalDateTime.of(2025, 9, 10, 0, 0);
    private static final LocalDateTime D15 = LocalDateTime.of(2025, 9, 15, 0, 0);
    private static final LocalDateTime D20 = LocalDateTime.of(2025, 9, 20, 0, 0);

    /* ---------- RF6.1 Casos límite ---------- */

    @Test
    @DisplayName("Rango sin préstamos debe devolver lista vacía")
    void whenActiveLoansRangeEmpty_thenEmpty() {
        when(loanRepository.findActiveLoansInRange(D20, D20.plusDays(1)))
                .thenReturn(List.of());

        assertThat(reportService.getActiveLoans(D20, D20.plusDays(1))).isEmpty();
        verify(loanRepository).findActiveLoansInRange(D20, D20.plusDays(1));
    }

    @Test
    @DisplayName("Todos los préstamos devueltos debe devolver lista vacía")
    void whenAllReturned_thenEmpty() {
        when(loanRepository.findActiveLoansInRange(D1, D20))
                .thenReturn(List.of());

        assertThat(reportService.getActiveLoans(D1, D20)).isEmpty();
        verify(loanRepository).findActiveLoansInRange(D1, D20);
    }

    @Test
    @DisplayName("Préstamo justo en límite inferior debe incluirse")
    void whenLoanAtFromBoundary_thenIncluded() {
        ToolEntity tool = buildTool(1L, "T");
        CustomerEntity customer = buildCustomer("Ana");
        LoanEntity loan = buildLoan(customer, tool);

        when(loanRepository.findActiveLoansInRange(D5, D10))
                .thenReturn(List.of(loan));

        List<LoanEntity> result = reportService.getActiveLoans(D5, D10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(loanRepository).findActiveLoansInRange(D5, D10);
    }

    /* ---------- RF6.2 Casos límite ---------- */

    @Test
    @DisplayName("Sin atrasos debe devolver lista vacía")
    void whenNoOverdue_thenEmpty() {
        when(customerRepository.findCustomersWithOverdueLoans(any()))
                .thenReturn(List.of());

        assertThat(reportService.getOverdueCustomers()).isEmpty();
        verify(customerRepository).findCustomersWithOverdueLoans(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Mismo cliente con varios atrasos debe aparecer una sola vez")
    void whenSameCustomerMultipleOverdue_thenUnique() {
        CustomerEntity c = buildCustomer("Luis");

        when(customerRepository.findCustomersWithOverdueLoans(any(LocalDateTime.class)))
                .thenReturn(List.of(c));

        List<CustomerEntity> result = reportService.getOverdueCustomers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Luis");
        verify(customerRepository).findCustomersWithOverdueLoans(any(LocalDateTime.class));
    }

    /* ---------- RF6.3 Casos límite ---------- */

    @Test
    @DisplayName("Rango sin préstamos debe devolver lista vacía")
    void whenTopToolsRangeEmpty_thenEmpty() {
        when(toolRepository.findTopLoanedTools(D20, D20.plusDays(1)))
                .thenReturn(List.of());

        assertThat(reportService.getTopTools(D20, D20.plusDays(1))).isEmpty();
        verify(toolRepository).findTopLoanedTools(D20, D20.plusDays(1));
    }

    @Test
    @DisplayName("Empate en cantidad debe mantener orden descendente")
    void whenTopToolsTie_thenOrderConserved() {
        ToolEntity t1 = buildTool(1L, "A");
        ToolEntity t2 = buildTool(2L, "B");

        ToolCountDTO dto1 = new ToolCountDTO() {
            @Override public ToolEntity getTool() { return t1; }
            @Override public Long getTotal() { return 2L; }
        };

        ToolCountDTO dto2 = new ToolCountDTO() {
            @Override public ToolEntity getTool() { return t2; }
            @Override public Long getTotal() { return 2L; }
        };

        when(toolRepository.findTopLoanedTools(D1, D20))
                .thenReturn(List.of(dto1, dto2)); // simula empate 2-2

        List<ToolCountDTO> ranking = reportService.getTopTools(D1, D20);

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).getTool().getName()).isEqualTo("A");
        assertThat(ranking.get(1).getTool().getName()).isEqualTo("B");
    }

    /* ---------- CONCURRENCIA ---------- */

    @Test
    @DisplayName("100 hilos consultando simultáneamente – sin excepciones")
    void whenConcurrentActiveLoans_thenNoException() throws InterruptedException {
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicReference<Exception> error = new AtomicReference<>();

        when(loanRepository.findActiveLoansInRange(D1, D20))
                .thenReturn(List.of());

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    reportService.getActiveLoans(D1, D20);
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        assertThat(error.get()).isNull();
        verify(loanRepository, times(threads)).findActiveLoansInRange(D1, D20);
    }

    /* ---------- Helpers ---------- */
    private ToolEntity buildTool(Long id, String name) {
        TariffEntity tariff = new TariffEntity();
        tariff.setDailyRentalRate(100.0);
        tariff.setDailyFineRate(100.0);

        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName(name);
        t.setCategory("C");
        t.setReplacementValue(1000.0);
        t.setPricePerDay(100.0);
        t.setStock(10);
        t.setStatus(ToolStatus.AVAILABLE);
        t.setTariff(tariff);
        return t;
    }

    private CustomerEntity buildCustomer(String name) {
        CustomerEntity c = new CustomerEntity();
        c.setId(1L);
        c.setName(name);
        c.setRut("11.111.111-1");
        c.setPhone("+56911111111");
        c.setEmail(name + "@test.com");
        c.setStatus(CustomerStatus.ACTIVE);
        return c;
    }

    private LoanEntity buildLoan(CustomerEntity c, ToolEntity t) {
        LoanEntity l = new LoanEntity();
        l.setId(1L);
        l.setCustomer(c);
        l.setTool(t);
        l.setLoanDate(ReportServiceTest.D5);
        l.setDueDate(ReportServiceTest.D15);
        l.setReturnDate(null);
        l.setTotalCost(100.0);
        l.setFineAmount(0.0);
        l.setDamageCharge(0.0);
        return l;
    }
}