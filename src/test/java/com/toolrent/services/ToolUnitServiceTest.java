package com.toolrent.services;

import com.toolrent.config.SecurityConfig;
import com.toolrent.entities.*;
import com.toolrent.repositories.LoanRepository;
import com.toolrent.repositories.ToolUnitRepository;
import com.toolrent.repositories.KardexMovementRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolUnitServiceTest {

    @Mock private ToolUnitRepository toolUnitRepository;
    @Mock private KardexMovementRepository kardexMovementRepository;
    @Mock private CustomerService customerService;
    @Mock private LoanRepository loanRepository;

    @InjectMocks private ToolUnitService toolUnitService;

    /* ======================================================================
              1. changeStatus
       ====================================================================== */

    @Test @DisplayName("changeStatus – transición válida (AVAILABLE -> IN_REPAIR)")
    void changeStatus_validTransition(){
        try (MockedStatic<SecurityConfig> mocked = mockStatic(SecurityConfig.class)){
            mocked.when(SecurityConfig::getCurrentUsername).thenReturn("emp1");

            ToolGroupEntity group = new ToolGroupEntity();
            group.setId(1L);
            ToolUnitEntity unit = new ToolUnitEntity();
            unit.setId(10L);
            unit.setStatus(ToolStatus.AVAILABLE);
            unit.setToolGroup(group);

            CustomerEntity system = new CustomerEntity();
            system.setId(0L);
            when(customerService.getSystemCustomer()).thenReturn(system);
            when(toolUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
            when(toolUnitRepository.save(unit)).thenReturn(unit);

            ToolUnitEntity res = toolUnitService.changeStatus(10L, ToolStatus.IN_REPAIR);

            assertThat(res.getStatus()).isEqualTo(ToolStatus.IN_REPAIR);
            verify(kardexMovementRepository).save(argThat(m ->
                    m.getMovementType() == MovementType.REPAIR));
        }
    }

    @Test @DisplayName("changeStatus – AVAILABLE → genera RE_ENTRY ")
    void changeStatus_toAvailable(){
        try (MockedStatic<SecurityConfig> mocked = mockStatic(SecurityConfig.class)){
            mocked.when(SecurityConfig::getCurrentUsername).thenReturn("emp1");

            ToolUnitEntity unit = new ToolUnitEntity();
            unit.setId(15L);
            unit.setStatus(ToolStatus.IN_REPAIR);

            CustomerEntity system = new CustomerEntity();
            system.setId(0L);
            when(customerService.getSystemCustomer()).thenReturn(system);
            when(toolUnitRepository.findById(15L)).thenReturn(Optional.of(unit));
            when(toolUnitRepository.save(unit)).thenReturn(unit);

            ToolUnitEntity res = toolUnitService.changeStatus(15L, ToolStatus.AVAILABLE);

            assertThat(res.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
            verify(kardexMovementRepository).save(argThat(m ->
                    m.getMovementType() == MovementType.RE_ENTRY));
        }
    }

    @Test @DisplayName("changeStatus – mismo estado → excepción")
    void changeStatus_sameStatus(){
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(10L);
        unit.setStatus(ToolStatus.AVAILABLE);
        when(toolUnitRepository.findById(10L)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> toolUnitService.changeStatus(10L, ToolStatus.AVAILABLE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya está en estado");
    }

    @Test @DisplayName("changeStatus – unidad retirada → excepción")
    void changeStatus_alreadyRetired(){
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(10L);
        unit.setStatus(ToolStatus.RETIRED);
        when(toolUnitRepository.findById(10L)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> toolUnitService.changeStatus(10L, ToolStatus.AVAILABLE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya fue retirada");
    }

    @ParameterizedTest
    @EnumSource(value = ToolStatus.class, names = {"LOANED", "IN_REPAIR", "RETIRED"})
    @DisplayName("changeStatus – no genera movimiento kardex")
    void changeStatus_noKardexMovement(ToolStatus target){
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(10L);
        unit.setStatus(ToolStatus.AVAILABLE);
        when(toolUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(toolUnitRepository.save(unit)).thenReturn(unit);

        ToolUnitEntity res = toolUnitService.changeStatus(10L, target);

        assertThat(res.getStatus()).isEqualTo(target);
        if (target == ToolStatus.LOANED) {
            verifyNoInteractions(kardexMovementRepository);
        }
    }

    @Test @DisplayName("changeStatus – LOANED → no genera movimiento kardex (rama default)")
    void changeStatus_loanedNoKardex(){
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(10L);
        unit.setStatus(ToolStatus.AVAILABLE);
        when(toolUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(toolUnitRepository.save(unit)).thenReturn(unit);

        ToolUnitEntity res = toolUnitService.changeStatus(10L, ToolStatus.LOANED);

        assertThat(res.getStatus()).isEqualTo(ToolStatus.LOANED);
        // verificamos que NO se intentó guardar movimiento
        verifyNoInteractions(kardexMovementRepository);
    }

    @Test @DisplayName("changeStatus – unidad no existe")
    void changeStatus_unitNotFound(){
        when(toolUnitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolUnitService.changeStatus(99L, ToolStatus.IN_REPAIR))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unidad no encontrada");
    }

    /* ======================================================================
              2. findAvailableUnit
       ====================================================================== */

    @Test @DisplayName("findAvailableUnit – existe")
    void findAvailableUnit_exists(){
        ToolGroupEntity group = new ToolGroupEntity();
        group.setId(1L);
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(20L);
        unit.setToolGroup(group);
        unit.setStatus(ToolStatus.AVAILABLE);
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(1L, ToolStatus.AVAILABLE))
                .thenReturn(Optional.of(unit));

        ToolUnitEntity res = toolUnitService.findAvailableUnit(1L);

        assertThat(res.getId()).isEqualTo(20L);
    }

    @Test @DisplayName("findAvailableUnit – no existe → excepción")
    void findAvailableUnit_notFound(){
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(1L, ToolStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolUnitService.findAvailableUnit(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay unidades disponibles");
    }

    /* ======================================================================
              3. retireFromRepair
       ====================================================================== */

    @Test @DisplayName("retireFromRepair – camino feliz")
    void retireFromRepair_ok(){
        try (MockedStatic<SecurityConfig> mocked = mockStatic(SecurityConfig.class)){
            mocked.when(SecurityConfig::getCurrentUsername).thenReturn("emp1");

            ToolGroupEntity group = new ToolGroupEntity();
            group.setId(1L);
            group.setReplacementValue(12000.0);

            ToolUnitEntity unit = new ToolUnitEntity();
            unit.setId(30L);
            unit.setStatus(ToolStatus.IN_REPAIR);
            unit.setToolGroup(group);

            LoanEntity loan = new LoanEntity();
            loan.setId(100L);
            loan.setReturnDate(LocalDateTime.now());

            when(toolUnitRepository.findById(30L)).thenReturn(Optional.of(unit));
            when(loanRepository.findTopByToolUnitIdAndReturnDateIsNotNullOrderByReturnDateDesc(30L))
                    .thenReturn(Optional.of(loan));
            when(toolUnitRepository.save(unit)).thenReturn(unit);

            toolUnitService.retireFromRepair(30L);

            assertThat(loan.getDamageCharge()).isEqualTo(12000.0);
            assertThat(unit.getStatus()).isEqualTo(ToolStatus.RETIRED);
            verify(loanRepository).save(loan);
        }
    }

    @Test @DisplayName("retireFromRepair – unidad no está en reparación")
    void retireFromRepair_notInRepair(){
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(30L);
        unit.setStatus(ToolStatus.AVAILABLE);
        when(toolUnitRepository.findById(30L)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> toolUnitService.retireFromRepair(30L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no está en reparación");
    }

    @Test @DisplayName("retireFromRepair – sin préstamo devuelto")
    void retireFromRepair_noReturnedLoan(){
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(30L);
        unit.setStatus(ToolStatus.IN_REPAIR);
        when(toolUnitRepository.findById(30L)).thenReturn(Optional.of(unit));
        when(loanRepository.findTopByToolUnitIdAndReturnDateIsNotNullOrderByReturnDateDesc(30L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolUnitService.retireFromRepair(30L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se encontró préstamo devuelto");
    }

    /* ======================================================================
              4. findById
       ====================================================================== */

    @Test @DisplayName("findById – existe")
    void findById_exists(){
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(40L);
        when(toolUnitRepository.findById(40L)).thenReturn(Optional.of(unit));

        ToolUnitEntity res = toolUnitService.findById(40L);

        assertThat(res).isEqualTo(unit);
    }

    @Test @DisplayName("findById – no existe → excepción")
    void findById_notFound(){
        when(toolUnitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolUnitService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unidad no encontrada");
    }

    /* ======================================================================
              5. findAllUnitsWithDetails
       ====================================================================== */

    @Test @DisplayName("findAllUnitsWithDetails – con datos")
    void findAllUnitsWithDetails_withData(){
        List<ToolUnitEntity> list = List.of(mock(ToolUnitEntity.class));
        when(toolUnitRepository.findAllWithToolGroup()).thenReturn(list);

        List<ToolUnitEntity> res = toolUnitService.findAllUnitsWithDetails();

        assertThat(res).hasSize(1);
    }

    @Test @DisplayName("findAllUnitsWithDetails – vacía")
    void findAllUnitsWithDetails_empty(){
        when(toolUnitRepository.findAllWithToolGroup()).thenReturn(List.of());

        List<ToolUnitEntity> res = toolUnitService.findAllUnitsWithDetails();

        assertThat(res).isEmpty();
    }

    /* ======================================================================
                  6. save
       ====================================================================== */

    @Test @DisplayName("save – ok")
    void save_ok(){
        ToolUnitEntity unit = new ToolUnitEntity();
        unit.setId(50L);
        when(toolUnitRepository.save(unit)).thenReturn(unit);

        ToolUnitEntity res = toolUnitService.save(unit);

        assertThat(res).isEqualTo(unit);
    }

    /* ======================================================================
                  7. getRealStock
       ====================================================================== */

    @Test @DisplayName("getRealStock – con unidades")
    void getRealStock_withData(){
        when(toolUnitRepository.countByToolGroupIdAndStatus(1L, ToolStatus.AVAILABLE)).thenReturn(7L);

        long stock = toolUnitService.getRealStock(1L);

        assertThat(stock).isEqualTo(7L);
    }

    @Test @DisplayName("getRealStock – sin unidades")
    void getRealStock_zero(){
        when(toolUnitRepository.countByToolGroupIdAndStatus(1L, ToolStatus.AVAILABLE)).thenReturn(0L);

        long stock = toolUnitService.getRealStock(1L);

        assertThat(stock).isZero();
    }
}