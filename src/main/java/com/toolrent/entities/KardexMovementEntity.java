package com.toolrent.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kardex_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KardexMovementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tool_unit_id", nullable = false)
    private ToolUnitEntity toolUnit;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer; // Cliente al que está asociado el movimiento

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private LocalDateTime movementDate = LocalDateTime.now();

    private String details; // Descripción del movimiento
}
