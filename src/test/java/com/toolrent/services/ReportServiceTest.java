package com.toolrent.services;

import com.toolrent.dto.CustomerDebtDTO;
import com.toolrent.dto.LoanActiveDTO;
import com.toolrent.entities.CustomerEntity;
import com.toolrent.repositories.CustomerRepository;
import com.toolrent.repositories.LoanRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private ReportService reportService;

    /* ======================================================================
              1. getActiveLoans – (vacía / con datos)
       ====================================================================== */

    @Test @DisplayName("getActiveLoans – con datos")
    void getActiveLoans_withData(){
        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 1, 31, 23, 59);
        List<LoanActiveDTO> data = List.of(mock(LoanActiveDTO.class), mock(LoanActiveDTO.class));
        when(loanRepository.findActiveLoansInRange(from, to)).thenReturn(data);

        List<LoanActiveDTO> res = reportService.getActiveLoans(from, to);

        assertThat(res).hasSize(2);
        verify(loanRepository).findActiveLoansInRange(from, to);
    }

    @Test @DisplayName("getActiveLoans – vacía")
    void getActiveLoans_empty(){
        LocalDateTime from = LocalDateTime.of(2025, 2, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 2, 1, 0, 0);
        when(loanRepository.findActiveLoansInRange(from, to)).thenReturn(List.of());

        List<LoanActiveDTO> res = reportService.getActiveLoans(from, to);

        assertThat(res).isEmpty();
    }

    /* ======================================================================
              2. getOverdueCustomers
       ====================================================================== */

    @Test @DisplayName("getOverdueCustomers – con datos")
    void getOverdueCustomers_withData(){
        CustomerEntity c1 = new CustomerEntity();
        c1.setId(1L);
        CustomerEntity c2 = new CustomerEntity();
        c2.setId(2L);
        List<CustomerEntity> data = List.of(c1, c2);
        when(customerRepository.findCustomersWithOverdueLoans(any(LocalDateTime.class))).thenReturn(data);

        List<CustomerEntity> res = reportService.getOverdueCustomers();

        assertThat(res).hasSize(2);
        verify(customerRepository).findCustomersWithOverdueLoans(any(LocalDateTime.class));
    }

    @Test @DisplayName("getOverdueCustomers – vacía")
    void getOverdueCustomers_empty(){
        when(customerRepository.findCustomersWithOverdueLoans(any(LocalDateTime.class))).thenReturn(List.of());

        List<CustomerEntity> res = reportService.getOverdueCustomers();

        assertThat(res).isEmpty();
    }

    /* ======================================================================
              3. getTopTools – (vacía / con datos / top 0 / top > size)
       ====================================================================== */

    @Test @DisplayName("getTopTools – con datos")
    void getTopTools_withData(){
        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 1, 31, 23, 59);
        List<Map<String, Object>> data = List.of(
                Map.of("toolGroupId", 1L, "toolName", "Taladro", "loanCount", 15L),
                Map.of("toolGroupId", 2L, "toolName", "Lijadora", "loanCount", 8L)
        );
        when(loanRepository.countLoansByToolGroupInRange(from, to)).thenReturn(data);

        List<Map<String, Object>> res = reportService.getTopTools(from, to);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).get("loanCount")).isEqualTo(15L);
    }

    @Test @DisplayName("getTopTools – vacía")
    void getTopTools_empty(){
        LocalDateTime from = LocalDateTime.of(2025, 2, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 2, 1, 0, 0);
        when(loanRepository.countLoansByToolGroupInRange(from, to)).thenReturn(List.of());

        List<Map<String, Object>> res = reportService.getTopTools(from, to);

        assertThat(res).isEmpty();
    }

    /* ======================================================================
              4. getCustomersWithDebt
       ====================================================================== */

    @Test @DisplayName("getCustomersWithDebt – con datos")
    void getCustomersWithDebt_withData(){
        LocalDateTime now = LocalDateTime.of(2025, 3, 15, 0, 0);
        List<CustomerDebtDTO> data = List.of(mock(CustomerDebtDTO.class));
        when(customerRepository.findCustomersWithDebt(now)).thenReturn(data);

        List<CustomerDebtDTO> res = reportService.getCustomersWithDebt(now);

        assertThat(res).hasSize(1);
        verify(customerRepository).findCustomersWithDebt(now);
    }

    @Test @DisplayName("getCustomersWithDebt – vacía")
    void getCustomersWithDebt_empty(){
        LocalDateTime now = LocalDateTime.of(2025, 3, 15, 0, 0);
        when(customerRepository.findCustomersWithDebt(now)).thenReturn(List.of());

        List<CustomerDebtDTO> res = reportService.getCustomersWithDebt(now);

        assertThat(res).isEmpty();
    }
}