package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.ToolUnitRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolUnitServiceTest {

    @Mock
    private ToolUnitRepository toolUnitRepository;

    @InjectMocks
    private ToolUnitService toolUnitService;

    /* ---------------------------------------------------------- */
    /* --------------------- CAMBIO DE ESTADO ------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Cambiar estado: disponible → prestado")
    void whenChangeStatus_AVAILABLE_to_LOANED_thenOk() {
        ToolUnitEntity unit = buildUnit();
        when(toolUnitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(toolUnitRepository.save(any(ToolUnitEntity.class))).thenReturn(unit);

        ToolUnitEntity result = toolUnitService.changeStatus(1L, ToolStatus.LOANED);

        assertThat(result.getStatus()).isEqualTo(ToolStatus.LOANED);
        verify(toolUnitRepository).save(unit);
    }

    @Test
    @DisplayName("Cambiar estado: null → se acepta y persiste")
    void whenChangeStatus_toNull_thenAccepts() {
        ToolUnitEntity unit = buildUnit();
        when(toolUnitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(toolUnitRepository.save(any(ToolUnitEntity.class))).thenReturn(unit);

        ToolUnitEntity result = toolUnitService.changeStatus(1L, null);

        assertThat(result.getStatus()).isNull();
        verify(toolUnitRepository).save(unit);
    }

    @ParameterizedTest
    @EnumSource(ToolStatus.class)
    @DisplayName("Cambiar estado: cualquier estado válido → OK")
    void whenChangeStatus_anyValidStatus_thenOk(ToolStatus status) {
        if (status == ToolStatus.AVAILABLE) {
            status = ToolStatus.LOANED; // evita misma excepción
        }

        ToolUnitEntity unit = buildUnit();
        when(toolUnitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(toolUnitRepository.save(any(ToolUnitEntity.class))).thenReturn(unit);

        ToolUnitEntity result = toolUnitService.changeStatus(1L, status);

        assertThat(result.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Cambiar estado: mismo estado → lanza excepción")
    void whenChangeStatus_sameStatus_thenThrows() {
        ToolUnitEntity unit = buildUnit();
        when(toolUnitRepository.findById(1L)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> toolUnitService.changeStatus(1L, ToolStatus.AVAILABLE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("La unidad ya está en estado: AVAILABLE");
    }

    @Test
    @DisplayName("Cambiar estado: unidad inexistente → excepción")
    void whenChangeStatus_unitNotFound_thenThrows() {
        when(toolUnitRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolUnitService.changeStatus(999L, ToolStatus.IN_REPAIR))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unidad no encontrada");
    }

    /* ---------------------------------------------------------- */
    /* --------------------- BUSCAR DISPONIBLE ------------------ */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Buscar disponible: hay unidad → la devuelve")
    void whenFindAvailable_exists_thenReturnsIt() {
        ToolUnitEntity unit = buildUnit();
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(1L, ToolStatus.AVAILABLE))
                .thenReturn(Optional.of(unit));

        ToolUnitEntity result = toolUnitService.findAvailableUnit(1L);

        assertThat(result).isEqualTo(unit);
    }

    @Test
    @DisplayName("Buscar disponible: no hay → excepción")
    void whenFindAvailable_none_thenThrows() {
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(1L, ToolStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolUnitService.findAvailableUnit(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay unidades disponibles");
    }

    /* ---------------------------------------------------------- */
    /* --------------------- CASOS EDGE ------------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Buscar disponible: grupo inexistente → excepción")
    void whenFindAvailable_groupNotFound_thenThrows() {
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(999L, ToolStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolUnitService.findAvailableUnit(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay unidades disponibles");
    }

    @Test
    @DisplayName("Buscar disponible: todas en reparación → excepción")
    void whenFindAvailable_allInRepair_thenThrows() {
        when(toolUnitRepository.findFirstByToolGroupIdAndStatus(1L, ToolStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolUnitService.findAvailableUnit(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay unidades disponibles");
    }

    @Test
    @DisplayName("Cambiar estado: ID null → excepción por NullPointer interno")
    void whenChangeStatus_nullId_thenThrows() {
        assertThatThrownBy(() -> toolUnitService.changeStatus(null, ToolStatus.LOANED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unidad no encontrada");
    }

    /* ---------------------------------------------------------- */
    /* --------------------- AUXILIARES ------------------------- */
    /* ---------------------------------------------------------- */

    private ToolUnitEntity buildUnit() {
        ToolUnitEntity u = new ToolUnitEntity();
        u.setId(1L);
        u.setStatus(ToolStatus.AVAILABLE);
        return u;
    }
}