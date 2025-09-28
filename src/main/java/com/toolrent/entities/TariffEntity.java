package com.toolrent.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tariffs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "tariff")
    @JsonIgnore
    private ToolGroupEntity tool_group;

    @NotNull(message = "La tarifa de alquiler diaria no puede ser nula")
    @Positive(message = "La tarifa de alquiler diaria debe ser mayor a 0")
    private Double dailyRentalRate;

    @NotNull(message = "La tarifa de multa diaria no puede ser nula")
    @Positive(message = "La tarifa de multa diaria debe ser mayor a 0")
    private Double dailyFineRate;

}