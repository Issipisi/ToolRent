package com.toolrent.repositories;

import com.toolrent.entities.KardexMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KardexMovementRepository extends JpaRepository<KardexMovementEntity, Long> {
    @Query("SELECT km FROM KardexMovementEntity km " +
            "JOIN FETCH km.toolUnit tu " +
            "JOIN FETCH tu.toolGroup tg " +
            "JOIN FETCH km.customer c " +
            "ORDER BY km.movementDate DESC")
    List<KardexMovementEntity> findAllWithDetails();

    /* RF5.2 – historial de UNA herramienta (por toolGroupId) */
    @Query("SELECT km FROM KardexMovementEntity km " +
            "JOIN FETCH km.toolUnit tu " +
            "JOIN FETCH tu.toolGroup tg " +
            "JOIN FETCH km.customer c " +
            "WHERE tg.id = :toolGroupId " +
            "ORDER BY km.movementDate DESC")
    List<KardexMovementEntity> findByToolGroupId(@Param("toolGroupId") Long toolGroupId);

    /* RF5.3 – movimientos entre dos fechas (por movementDate) */
    @Query("SELECT km FROM KardexMovementEntity km " +
            "JOIN FETCH km.toolUnit tu " +
            "JOIN FETCH tu.toolGroup tg " +
            "JOIN FETCH km.customer c " +
            "WHERE km.movementDate BETWEEN :from AND :to " +
            "ORDER BY km.movementDate DESC")
    List<KardexMovementEntity> findByDateRange(@Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);
}
