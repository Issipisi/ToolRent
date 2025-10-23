package com.toolrent.services;

import com.toolrent.dto.CustomerDebtDTO;
import com.toolrent.dto.LoanActiveDTO;
import com.toolrent.entities.CustomerEntity;
import com.toolrent.repositories.LoanRepository;
import com.toolrent.repositories.CustomerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;

    public ReportService(LoanRepository loanRepository,
                         CustomerRepository customerRepository) {
        this.loanRepository = loanRepository;
        this.customerRepository = customerRepository;
    }

    /* Préstamos activos (sin devolver) en rango de loanDate */
    public List<LoanActiveDTO> getActiveLoans(LocalDateTime from, LocalDateTime to) {
        return loanRepository.findActiveLoansInRange(from, to);
    }

    /* Clientes con al menos un préstamo atrasado */
    public List<CustomerEntity> getOverdueCustomers() {
        return customerRepository.findCustomersWithOverdueLoans(LocalDateTime.now());
    }

    /* Ranking de herramientas más prestadas en rango de loanDate */
    public List<Map<String, Object>> getTopTools(LocalDateTime from, LocalDateTime to) {
        return loanRepository.countLoansByToolGroupInRange(from, to);
    }

    /* Clientes con deudas */
    public List<CustomerDebtDTO> getCustomersWithDebt(LocalDateTime now) {
        return customerRepository.findCustomersWithDebt(now);
    }
}