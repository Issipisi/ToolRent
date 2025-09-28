package com.toolrent.repositories;

import com.toolrent.entities.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    // Préstamos activos (sin devolución) en rango de fecha de préstamo
    @Query("SELECT l FROM LoanEntity l " +
            "WHERE l.returnDate IS NULL " +
            "AND l.loanDate BETWEEN :from AND :to " +
            "ORDER BY l.dueDate ASC")
    List<LoanEntity> findActiveLoansInRange(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    // Ranking de grupos más prestados en un rango de fechas
    @Query(value = """
    SELECT tg.id   AS toolGroupId,
           tg.name AS toolGroupName,
           COUNT(l.id) AS total
    FROM loans l
    JOIN tool_units tu ON l.tool_unit_id = tu.id
    JOIN tool_groups tg ON tu.tool_group_id = tg.id
    WHERE l.loan_date BETWEEN :from AND :to
    GROUP BY tg.id, tg.name
    ORDER BY total DESC
    """, nativeQuery = true)
    List<Map<String, Object>> countLoansByToolGroupInRange(@Param("from") LocalDateTime from,
                                                           @Param("to") LocalDateTime to);

}