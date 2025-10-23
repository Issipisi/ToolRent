package com.toolrent.dto;

import java.time.LocalDateTime;

public record LoanActiveDTO(
        Long id,
        String customerName,
        String toolName,
        LocalDateTime loanDate,
        LocalDateTime dueDate,
        LocalDateTime returnDate,
        Double fineAmount,
        Double damageCharge,
        String status
) {
    public LoanActiveDTO(Long id, String customerName, String toolName,
                         LocalDateTime loanDate, LocalDateTime dueDate,
                         LocalDateTime returnDate, Double fineAmount, Double damageCharge) {
        this(id, customerName, toolName, loanDate, dueDate, returnDate,
                fineAmount, damageCharge,
                returnDate == null ? "ACTIVE" : "RETURNED");
    }
}