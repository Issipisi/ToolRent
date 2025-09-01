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

    @Enumerated(EnumType.STRING)  // Almacena enum como string (e.g., "AVAILABLE")
    private ToolStatus status = ToolStatus.AVAILABLE;  // Default

    @Column(nullable = false)
    private Double replacementValue;    //valor de reposición

    private Integer stock = 1;  // Default
}