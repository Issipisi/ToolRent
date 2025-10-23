package com.toolrent.dto;

import java.time.LocalDateTime;

public record CustomerDebtDTO(
        Long customerId,
        String name,
        String rut,
        String email,
        Double totalDebt, // multa + daño
        Boolean hasOverdueLoan, // préstamo sin devolver
        LocalDateTime oldestDueDate // más antiguo sin devolver
) {}