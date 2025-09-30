package com.toolrent.config;

import java.time.LocalDateTime;

public record LoanActiveDTO(
        Long id,
        String customerName,
        String toolName,
        LocalDateTime loanDate,
        LocalDateTime dueDate,
        String status   // se calculará
) {
    // Constructor compacto: se ejecuta después de los JOIN
    public LoanActiveDTO(Long id, String customerName, String toolName,
                         LocalDateTime loanDate, LocalDateTime dueDate,
                         LocalDateTime returnDate) {
        this(id, customerName, toolName, loanDate, dueDate,
                returnDate == null ? "ACTIVE" : "RETURNED");
    }
}