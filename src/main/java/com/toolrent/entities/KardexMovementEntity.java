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
    @JoinColumn(name = "tool_id", nullable = false)
    private ToolEntity tool;

    @Enumerated(EnumType.STRING)
    private MovementType type;

    private LocalDateTime date = LocalDateTime.now();

    private Long userId;

    private Integer quantity;
}
