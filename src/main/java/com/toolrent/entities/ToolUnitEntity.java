package com.toolrent.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@ToString(exclude = "toolGroup")
@Entity
@Table(name = "tool_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolUnitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tool_group_id", nullable = false)
    @JsonIgnoreProperties({"units", "tariff", "replacementValue", "category"})
    private ToolGroupEntity toolGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToolStatus status = ToolStatus.AVAILABLE;

    @OneToMany(mappedBy = "toolUnit")
    @JsonIgnore
    private List<LoanEntity> loans = new ArrayList<>();
}