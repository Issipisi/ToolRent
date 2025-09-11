package com.toolrent.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tools")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToolStatus status = ToolStatus.AVAILABLE; // Default: AVAILABLE

    @Column(nullable = false)
    private Double replacementValue; // Valor de reposición

    @Column(nullable = false)
    private Double pricePerDay; // Precio por día de alquiler

    @Column(nullable = false)
    private Integer stock = 0; // Default: 0, se ajusta al agregar herramientas

    @Column(updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date createdAt = new java.util.Date(); // Fecha de creación
}