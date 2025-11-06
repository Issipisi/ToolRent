package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.ToolGroupRepository;
import com.toolrent.repositories.ToolUnitRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolGroupServiceTest {

    @Mock private ToolGroupRepository toolGroupRepository;
    @Mock private KardexMovementService kardexMovementService;
    @Mock private ToolUnitRepository toolUnitRepository;

    @InjectMocks private ToolGroupService toolGroupService;

    /* ======================================================================
                  1. registerToolGroup
       ====================================================================== */

    @Test @DisplayName("registerToolGroup – camino feliz")
    void registerToolGroup_ok(){
        // devuelve la misma instancia que recibe (ya contiene las unidades)
        when(toolGroupRepository.save(any(ToolGroupEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToolGroupEntity res = toolGroupService.registerToolGroup(
                "Taladro", "Electricidad", 15000.0, 1000.0, 3);

        assertThat(res.getUnits()).hasSize(3);
        assertThat(res.getUnits()).allMatch(u -> u.getStatus() == ToolStatus.AVAILABLE);
        verify(kardexMovementService).saveRegistryKardex(res, 3);
    }


    @ParameterizedTest
    @NullSource
    @DisplayName("registerToolGroup – nombre null → excepción")
    void registerToolGroup_nullName(String name){
        assertThatThrownBy(() -> toolGroupService.registerToolGroup(
                name, "Cat", 1000.0, 500.0, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("registerToolGroup – nombre blank → excepción")
    void registerToolGroup_blankName(String name){
        assertThatThrownBy(() -> toolGroupService.registerToolGroup(
                name, "Cat", 1000.0, 500.0, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("registerToolGroup – categoría null → excepción")
    void registerToolGroup_nullCategory(String category){
        assertThatThrownBy(() -> toolGroupService.registerToolGroup(
                "Herramienta", category, 1000.0, 500.0, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("registerToolGroup – categoría blank → excepción")
    void registerToolGroup_blankCategory(String category){
        assertThatThrownBy(() -> toolGroupService.registerToolGroup(
                "Herramienta", category, 1000.0, 500.0, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @Test @DisplayName("registerToolGroup – replacementValue null → excepción")
    void registerToolGroup_nullReplacement(){
        assertThatThrownBy(() -> toolGroupService.registerToolGroup(
                "Herramienta", "Cat", null, 500.0, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10})
    @DisplayName("registerToolGroup – stock negativo → excepción")
    void registerToolGroup_negativeStock(int stock){
        assertThatThrownBy(() -> toolGroupService.registerToolGroup(
                "Herramienta", "Cat", 1000.0, 500.0, stock))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("stock no puede ser negativo");
    }

    @Test @DisplayName("registerToolGroup – stock 0 → permite sin unidades")
    void registerToolGroup_zeroStock(){
        ToolGroupEntity saved = new ToolGroupEntity();
        saved.setId(2L);
        when(toolGroupRepository.save(any(ToolGroupEntity.class))).thenReturn(saved);

        ToolGroupEntity res = toolGroupService.registerToolGroup(
                "Martillo", "Manual", 5000.0, 300.0, 0);

        assertThat(res.getUnits()).isEmpty();
        verify(kardexMovementService).saveRegistryKardex(res, 0);
    }

    @Test @DisplayName("registerToolGroup – stock 1 → una unidad")
    void registerToolGroup_oneUnit(){
        when(toolGroupRepository.save(any(ToolGroupEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToolGroupEntity res = toolGroupService.registerToolGroup(
                "Sierra", "Corte", 8000.0, 600.0, 1);

        assertThat(res.getUnits()).hasSize(1);
        verify(kardexMovementService).saveRegistryKardex(res, 1);
    }

    /* ====================================================================== */
    /*  2. getAllToolGroups – 100 % line                                      */
    /* ====================================================================== */

    @Test @DisplayName("getAllToolGroups – con datos")
    void getAllToolGroups_withData(){
        List<ToolGroupEntity> list = List.of(mock(ToolGroupEntity.class));
        when(toolGroupRepository.findAll()).thenReturn(list);

        Iterable<ToolGroupEntity> res = toolGroupService.getAllToolGroups();

        assertThat(res).hasSize(1);
    }

    @Test @DisplayName("getAllToolGroups – vacía")
    void getAllToolGroups_empty(){
        when(toolGroupRepository.findAll()).thenReturn(List.of());

        Iterable<ToolGroupEntity> res = toolGroupService.getAllToolGroups();

        assertThat(res).isEmpty();
    }

    /* ======================================================================
          3. getToolGroupsWithAvailableUnits
       ====================================================================== */

    @Test @DisplayName("getToolGroupsWithAvailableUnits – filtra solo con disponibles")
    void getToolGroupsWithAvailableUnits_filter(){
        ToolGroupEntity g1 = buildGroupWithUnits(1L, ToolStatus.AVAILABLE, ToolStatus.LOANED);
        ToolGroupEntity g2 = buildGroupWithUnits(2L, ToolStatus.LOANED, ToolStatus.LOANED);
        List<ToolGroupEntity> all = List.of(g1, g2);
        when(toolGroupRepository.findAll()).thenReturn(all);

        List<ToolGroupEntity> res = toolGroupService.getToolGroupsWithAvailableUnits();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getId()).isEqualTo(1L);
    }

    @Test @DisplayName("getToolGroupsWithAvailableUnits – ninguna disponible → vacía")
    void getToolGroupsWithAvailableUnits_none(){
        ToolGroupEntity g1 = buildGroupWithUnits(1L, ToolStatus.LOANED, ToolStatus.IN_REPAIR);
        List<ToolGroupEntity> all = List.of(g1);
        when(toolGroupRepository.findAll()).thenReturn(all);

        List<ToolGroupEntity> res = toolGroupService.getToolGroupsWithAvailableUnits();

        assertThat(res).isEmpty();
    }

    /* ======================================================================
          4. findById
       ====================================================================== */

    @Test @DisplayName("findById – existe")
    void findById_exists(){
        ToolGroupEntity group = new ToolGroupEntity();
        group.setId(10L);
        when(toolGroupRepository.findById(10L)).thenReturn(Optional.of(group));

        ToolGroupEntity res = toolGroupService.findById(10L);

        assertThat(res).isEqualTo(group);
    }

    @Test @DisplayName("findById – no existe → excepción")
    void findById_notFound(){
        when(toolGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolGroupService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ToolGroup not found");
    }

    /* ======================================================================
              5. save – 100 % line
       ====================================================================== */

    @Test @DisplayName("save – ok")
    void save_ok(){
        ToolGroupEntity group = new ToolGroupEntity();
        group.setId(20L);
        when(toolGroupRepository.save(group)).thenReturn(group);

        ToolGroupEntity res = toolGroupService.save(group);

        assertThat(res).isEqualTo(group);
    }

    /* ======================================================================
                                      Helpers
       ====================================================================== */

    private ToolGroupEntity buildGroupWithUnits(Long id, ToolStatus... statuses){
        ToolGroupEntity g = new ToolGroupEntity();
        g.setId(id);
        for (ToolStatus s : statuses) {
            ToolUnitEntity u = new ToolUnitEntity();
            u.setStatus(s);
            u.setToolGroup(g);
            g.getUnits().add(u);
        }
        return g;
    }
}