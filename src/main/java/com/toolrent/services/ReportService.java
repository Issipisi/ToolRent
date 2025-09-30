package com.toolrent.services;

import com.toolrent.entities.LoanEntity;
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

    /* RF6.1 Préstamos activos (sin devolver) en rango de loanDate */
    public List<LoanEntity> getActiveLoans(LocalDateTime from, LocalDateTime to) {
        return loanRepository.findActiveLoansInRangeReport(from, to);
    }

    /* RF6.2 Clientes con al menos un préstamo atrasado */
    public List<CustomerEntity> getOverdueCustomers() {
        return customerRepository.findCustomersWithOverdueLoans(LocalDateTime.now());
    }

    /* RF6.3 Ranking de grupos más prestados en rango de loanDate */
    public List<Map<String, Object>> getTopTools(LocalDateTime from, LocalDateTime to) {
        return loanRepository.countLoansByToolGroupInRange(from, to);
    }
}