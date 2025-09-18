package com.toolrent.repositories;

import com.toolrent.config.ToolCountDTO;
import com.toolrent.entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<ToolEntity, Long> {
    //Herramientas más prestadas
    @Query("SELECT l.tool AS tool, COUNT(l) AS total " +
            "FROM LoanEntity l " +
            "WHERE l.loanDate BETWEEN :from AND :to " +  // ← cambio aquí
            "GROUP BY l.tool " +
            "ORDER BY total DESC")
    List<ToolCountDTO> findTopLoanedTools(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}