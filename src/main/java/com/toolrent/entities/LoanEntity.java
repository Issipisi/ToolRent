package com.toolrent.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "loans") //prestamos
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;  // Relación

    @ManyToOne
    @JoinColumn(name = "tool_id", nullable = false)
    private ToolEntity tool;

    private LocalDateTime loanDate;     //Fecha prestamo
    private LocalDateTime dueDate;      //Fecha de vencimiento
    private LocalDateTime returnDate;

    private Double fineAmount = 0.0;    //multa
    private Double damageCharge = 0.0;  //daño
}