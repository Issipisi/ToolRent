package com.toolrent.repositories;

import com.toolrent.dto.CustomerDebtDTO;
import com.toolrent.entities.CustomerEntity;
import com.toolrent.entities.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByEmail(String mail);

    // Clientes con atrasos
    @Query("SELECT DISTINCT c FROM LoanEntity l JOIN l.customer c " +
            "WHERE l.returnDate IS NULL AND l.dueDate < :now")
    List<CustomerEntity> findCustomersWithOverdueLoans(@Param("now") LocalDateTime now);

    // Clientes con deudas
    @Query("""
    SELECT new com.toolrent.dto.CustomerDebtDTO(
           c.id,
           c.name,
           c.rut,
           c.email,
           CAST(COALESCE(SUM(l.fineAmount + l.damageCharge), 0) AS double),
           CASE WHEN COUNT(l2) > 0 THEN TRUE ELSE FALSE END,
           MIN(l2.dueDate)
    )
    FROM CustomerEntity c
    LEFT JOIN LoanEntity l ON l.customer.id = c.id
       AND l.returnDate IS NOT NULL
       AND (l.fineAmount > 0 OR l.damageCharge > 0)
    LEFT JOIN LoanEntity l2 ON l2.customer.id = c.id
       AND l2.returnDate IS NULL
       AND l2.dueDate < :now
    GROUP BY c.id, c.name, c.rut, c.email
    HAVING (COALESCE(SUM(l.fineAmount + l.damageCharge), 0) > 0
            OR COUNT(l2) > 0)
    ORDER BY COALESCE(SUM(l.fineAmount + l.damageCharge), 0) DESC
""")
    List<CustomerDebtDTO> findCustomersWithDebt(@Param("now") LocalDateTime now);

    // Encontrar por estado
    List<CustomerEntity> findByStatus(CustomerStatus status);
}
