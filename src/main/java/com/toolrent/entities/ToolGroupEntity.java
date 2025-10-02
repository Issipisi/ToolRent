package com.toolrent.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tool_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double replacementValue;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "tariff_id", nullable = false)
    private TariffEntity tariff;

    @OneToMany(mappedBy = "toolGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ToolUnitEntity> units = new ArrayList<>();
}