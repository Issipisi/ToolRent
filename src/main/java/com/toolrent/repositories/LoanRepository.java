package com.toolrent.repositories;

import com.toolrent.entities.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    //Préstamos activos
    @Query("SELECT l FROM LoanEntity l WHERE l.returnDate IS NULL " +
            "AND l.loanDate BETWEEN :from AND :to " +
            "ORDER BY l.dueDate ASC")
    List<LoanEntity> findActiveLoansInRange(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    //Préstamos atrasados
    @Query("SELECT l FROM LoanEntity l WHERE l.returnDate IS NULL " +
            "AND l.dueDate < :now")
    List<LoanEntity> findOverdueLoans(@Param("now") LocalDateTime now);
}
