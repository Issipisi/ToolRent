package com.toolrent.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne
    @JoinColumn(name = "tool_id", nullable = false)
    private ToolEntity tool;

    @Column(nullable = false)
    private LocalDateTime loanDate = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime dueDate;

    private LocalDateTime returnDate;

    private Double totalCost; // Calculado según días y pricePerDay

    private Double fineAmount = 0.0; // Multa por retraso
    private Double damageCharge = 0.0; // Cargo por daño
}