package com.toolrent.services;

import com.toolrent.entities.ToolEntity;
import com.toolrent.entities.ToolStatus;
import com.toolrent.repositories.ToolRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    @InjectMocks
    private ToolService toolService;

    /*-------- REGISTER TOOL --------*/

    @Test
    void whenRegisterTool_withValidData_thenSuccess() {
        ToolEntity saved = buildTool(1L, "Taladro", "Eléctrica", 150.0, 15.0, ToolStatus.AVAILABLE);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(saved);

        ToolEntity result = toolService.registerTool("Taladro", "Eléctrica", 150.0, 15.0);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Taladro");
        assertThat(result.getCategory()).isEqualTo("Eléctrica");
        assertThat(result.getReplacementValue()).isEqualTo(150.0);
        assertThat(result.getPricePerDay()).isEqualTo(15.0);
        assertThat(result.getStock()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
        verify(toolRepository).save(any(ToolEntity.class));
    }

    /* -------- NULLOS -------- */
    @ParameterizedTest
    @NullSource
    void whenRegisterTool_withNullName_thenThrows(String name) {
        assertThatThrownBy(() -> toolService.registerTool(name, "cat", 100.0, 10.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
        verifyNoInteractions(toolRepository);
    }

    @ParameterizedTest
    @NullSource
    void whenRegisterTool_withNullCategory_thenThrows(String category) {
        assertThatThrownBy(() -> toolService.registerTool("name", category, 100.0, 10.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @ParameterizedTest
    @NullSource
    void whenRegisterTool_withNullReplacementValue_thenThrows(Double replacementValue) {
        assertThatThrownBy(() -> toolService.registerTool("name", "cat", replacementValue, 10.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    /* -------- VALORES EXTREMOS -------- */
    @Test
    void whenRegisterTool_withMaxValues_thenOk() {
        String bigName = "X".repeat(255);
        String bigCat = "Y".repeat(100);
        double maxReplacement = 1_000_000.0;
        double maxPrice = 99_999.99;

        ToolEntity saved = buildTool(1L, bigName, bigCat, maxReplacement, maxPrice, ToolStatus.AVAILABLE);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(saved);

        ToolEntity result = toolService.registerTool(bigName, bigCat, maxReplacement, maxPrice);

        assertThat(result.getName()).hasSize(255);
        assertThat(result.getCategory()).hasSize(100);
        assertThat(result.getReplacementValue()).isEqualTo(maxReplacement);
        assertThat(result.getPricePerDay()).isEqualTo(maxPrice);
    }

    @Test
    void whenRegisterTool_withZeroPrice_thenStillSaves() {
        ToolEntity saved = buildTool(1L, "A", "B", 10.0, 0.0, ToolStatus.AVAILABLE);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(saved);

        ToolEntity result = toolService.registerTool("A", "B", 10.0, 0.0);

        assertThat(result.getPricePerDay()).isZero();
    }

    @Test
    void whenRegisterTool_withNegativePrice_thenStillSaves() {
        ToolEntity saved = buildTool(1L, "A", "B", 10.0, -5.0, ToolStatus.AVAILABLE);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(saved);

        ToolEntity result = toolService.registerTool("A", "B", 10.0, -5.0);

        assertThat(result.getPricePerDay()).isEqualTo(-5.0);
    }

    /* -------- STOCK POR DEFECTO -------- */
    @Test
    void whenRegisterTool_withoutStockParam_thenDefaultsToOne() {
        ToolEntity saved = buildTool(1L, "H", "C", 50.0, 5.0, ToolStatus.AVAILABLE);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(saved);

        ToolEntity result = toolService.registerTool("H", "C", 50.0, 5.0);

        assertThat(result.getStock()).isEqualTo(1);
    }

    /*-------- CHANGE STATE TOOL --------*/

    @Test
    void whenChangeStatus_toLoaned_thenSuccess() {
        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0,
                ToolStatus.AVAILABLE);

        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(tool); // ← devuelve la misma

        ToolEntity result = toolService.changeStatus(1L, ToolStatus.LOANED);

        assertThat(result.getStatus()).isEqualTo(ToolStatus.LOANED);
        verify(toolRepository).save(tool);
    }

    @Test
    void whenChangeStatus_toRetired_thenThrows() {
        assertThatThrownBy(() -> toolService.changeStatus(1L, ToolStatus.RETIRED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error");
    }

    /*-------- DESACTIVATE TOOL --------*/

    @Test
    void whenDesactivateTool_withExistingId_thenSetsStatusRetired() {
        Long id = 1L;
        ToolEntity tool = buildTool(id, "T", "C", 100.0, 10.0, ToolStatus.AVAILABLE);
        when(toolRepository.findById(id)).thenReturn(Optional.of(tool));

        toolService.desactivateTool(id);

        assertThat(tool.getStatus()).isEqualTo(ToolStatus.RETIRED);
        verify(toolRepository).save(tool);
    }

    @Test
    void whenDesactivateTool_withNegativeId_thenThrows() {
        Long id = -5L;
        when(toolRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolService.desactivateTool(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tool not found");
    }

    @Test
    void whenDesactivateTool_withNotFoundId_thenThrows() {
        Long id = 9999L;
        when(toolRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolService.desactivateTool(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tool not found");
    }

    /*-------- GET ALL TOOLS --------*/

    @Test
    void whenGetAllTools_thenReturnsList() {
        List<ToolEntity> data = List.of(
                buildTool(1L, "A", "C1", 10.0, 1.0, ToolStatus.AVAILABLE),
                buildTool(2L, "B", "C2", 20.0, 2.0, ToolStatus.RETIRED)
        );
        when(toolRepository.findAll()).thenReturn(data);

        Iterable<ToolEntity> result = toolService.getAllTools();

        assertThat(result).hasSize(2);
        verify(toolRepository).findAll();
    }

    @Test
    void whenGetAllTools_emptyRepo_thenReturnsEmpty() {
        when(toolRepository.findAll()).thenReturn(List.of());

        Iterable<ToolEntity> result = toolService.getAllTools();

        assertThat(result).isEmpty();
    }

    /*-------- HELPER --------*/

    private ToolEntity buildTool(Long id, String name, String category, Double replacementValue,
                                 Double pricePerDay, ToolStatus status) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName(name);
        t.setCategory(category);
        t.setReplacementValue(replacementValue);
        t.setPricePerDay(pricePerDay);
        t.setStock(1);
        t.setStatus(status);
        return t;
    }
}