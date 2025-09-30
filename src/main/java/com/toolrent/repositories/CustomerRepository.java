package com.toolrent.repositories;

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

    //Clientes con atrasos
    @Query("SELECT DISTINCT c FROM LoanEntity l JOIN l.customer c " +
            "WHERE l.returnDate IS NULL AND l.dueDate < :now")
    List<CustomerEntity> findCustomersWithOverdueLoans(@Param("now") LocalDateTime now);

    List<CustomerEntity> findByStatus(CustomerStatus status);
}
