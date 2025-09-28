package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.KardexMovementRepository;
import com.toolrent.repositories.LoanRepository;
import com.toolrent.repositories.CustomerRepository;
import com.toolrent.repositories.ToolUnitRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private ToolUnitRepository toolUnitRepository;

    @Mock
    private KardexMovementRepository kardexMovementRepository;

    @InjectMocks
    private ReportService reportService;

    @InjectMocks
    private LoanService loanService;

    /* Fechas fijas reproducibles */
    private static final LocalDateTime D1  = LocalDateTime.of(2025, 9, 1, 0, 0);
    private static final LocalDateTime D5  = LocalDateTime.of(2025, 9, 5, 0, 0);
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
    @DisplayName("Devolución atrasada: calcula multa")
    void whenReturnLoan_withDelay_thenAppliesFine() {
        Long loanId = 1L;
        LocalDateTime dueDate = LocalDateTime.now().minusDays(3);

        ToolGroupEntity group = buildToolGroup();

        // Crea y añade una unidad manualmente
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(1L);
        unit.setStatus(ToolStatus.LOANED);
        unit.setToolGroup(group);
        group.getUnits().add(unit);

        CustomerEntity customer = buildCustomer("Ana");
        LoanEntity loan = buildLoan(customer, unit);
        loan.setDueDate(dueDate);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        loanService.returnLoan(loanId);

        assertThat(unit.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
        assertThat(loan.getReturnDate()).isNotNull();
        assertThat(loan.getFineAmount()).isEqualTo(600.0);

        verify(toolUnitRepository).save(unit);
        verify(loanRepository).save(loan);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
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
        when(loanRepository.countLoansByToolGroupInRange(D20, D20.plusDays(1)))
                .thenReturn(List.of());

        assertThat(reportService.getTopTools(D20, D20.plusDays(1))).isEmpty();
        verify(loanRepository).countLoansByToolGroupInRange(D20, D20.plusDays(1));
    }

    @Test
    @DisplayName("Empate en cantidad debe mantener orden descendente")
    void whenTopToolsTie_thenOrderConserved() {
        Map<String, Object> row1 = Map.of(
                "toolGroupId", 1L,
                "toolGroupName", "A",
                "total", 5L
        );
        Map<String, Object> row2 = Map.of(
                "toolGroupId", 2L,
                "toolGroupName", "B",
                "total", 5L
        );

        when(loanRepository.countLoansByToolGroupInRange(D1, D20))
                .thenReturn(List.of(row1, row2));

        List<Map<String, Object>> ranking = reportService.getTopTools(D1, D20);

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).get("toolGroupId")).isEqualTo(1L);
        assertThat(ranking.get(1).get("toolGroupId")).isEqualTo(2L);
    }

    @Test
    @DisplayName("Ranking con valores extremos (total = 0 y total = 999999)")
    void whenTopTools_withExtremeTotals_thenAccepted() {
        Map<String, Object> row1 = Map.of(
                "toolGroupId", 1L,
                "toolGroupName", "Zero",
                "total", 0L
        );
        Map<String, Object> row2 = Map.of(
                "toolGroupId", 2L,
                "toolGroupName", "Huge",
                "total", 999_999L
        );

        when(loanRepository.countLoansByToolGroupInRange(D1, D20))
                .thenReturn(List.of(row1, row2));

        List<Map<String, Object>> ranking = reportService.getTopTools(D1, D20);

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).get("total")).isEqualTo(0L);
        assertThat(ranking.get(1).get("total")).isEqualTo(999_999L);
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
    private ToolGroupEntity buildToolGroup() {
        TariffEntity tariff = new TariffEntity();
        tariff.setDailyRentalRate(100.0);
        tariff.setDailyFineRate(100.0 * 2);

        ToolGroupEntity g = new ToolGroupEntity();
        g.setId(1L);
        g.setName("T");
        g.setCategory("C");
        g.setReplacementValue(1000.0);
        g.setTariff(tariff);
        return g;
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

    private LoanEntity buildLoan(CustomerEntity c, ToolUnitEntity unit) {
        LoanEntity l = new LoanEntity();
        l.setId(1L);
        l.setCustomer(c);
        l.setToolUnit(unit);
        l.setLoanDate(D5);
        l.setDueDate(D15);
        l.setReturnDate(null);
        l.setTotalCost(100.0);
        l.setFineAmount(0.0);
        l.setDamageCharge(0.0);
        return l;
    }
}