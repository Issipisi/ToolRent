package com.toolrent.services;

import com.toolrent.config.ToolCountDTO;
import com.toolrent.entities.LoanEntity;
import com.toolrent.entities.CustomerEntity;
import com.toolrent.repositories.LoanRepository;
import com.toolrent.repositories.CustomerRepository;
import com.toolrent.repositories.ToolRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final ToolRepository toolRepository;

    public ReportService(LoanRepository loanRepository,
                         CustomerRepository customerRepository,
                         ToolRepository toolRepository) {
        this.loanRepository = loanRepository;
        this.customerRepository = customerRepository;
        this.toolRepository = toolRepository;
    }

    /* RF6.1 Préstamos activos (sin devolver) en rango de loanDate */
    public List<LoanEntity> getActiveLoans(LocalDateTime from, LocalDateTime to) {
        return loanRepository.findActiveLoansInRange(from, to);
    }

    /* RF6.2 Clientes con al menos un préstamo atrasado */
    public List<CustomerEntity> getOverdueCustomers() {
        return customerRepository.findCustomersWithOverdueLoans(LocalDateTime.now());
    }

    /* RF6.3 Ranking de herramientas más prestadas en rango de loanDate */
    public List<ToolCountDTO> getTopTools(LocalDateTime from, LocalDateTime to) {
        return toolRepository.findTopLoanedTools(from, to);
    }
}