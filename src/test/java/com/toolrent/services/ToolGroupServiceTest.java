package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolGroupServiceTest {

    @Mock
    private ToolGroupRepository toolGroupRepository;

    @Mock
    private TariffRepository tariffRepository;

    @Mock
    private KardexMovementRepository kardexMovementRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ToolGroupService toolGroupService;

    /* ---------------------------------------------------------- */
    /* --------------------- REGISTRO --------------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Registrar grupo: stock 0 → crea grupo sin unidades")
    void whenRegister_withZeroStock_thenNoUnits() {
        ToolGroupEntity group = buildGroup("Martillo", "Manual", 5000.0, 1000.0, 0);
        when(toolGroupRepository.save(any(ToolGroupEntity.class))).thenReturn(group);

        ToolGroupEntity result = toolGroupService.registerToolGroup("Martillo", "Manual", 5000.0, 1000.0, 0);

        assertThat(result.getUnits()).isEmpty();
        verify(toolGroupRepository).save(any(ToolGroupEntity.class));
    }

    @Test
    @DisplayName("Registrar grupo: stock negativo → excepción")
    void whenRegister_withNegativeStock_thenThrows() {
        assertThatThrownBy(() -> toolGroupService.registerToolGroup("A", "B", 1000.0, 100.0, -5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El stock no puede ser negativo");
        verifyNoInteractions(toolGroupRepository);
    }

    @ParameterizedTest(name = "name=''{0}'' → debe fallar")
    @NullSource
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("Registrar grupo: nombre nulo o blanco → excepción")
    void whenRegister_withNullOrBlankName_thenThrows(String name) {
        assertThatThrownBy(() -> toolGroupService.registerToolGroup(name, "Cat", 1000.0, 100.0, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @Test
    @DisplayName("Registrar grupo: valor de reposición 0 → se acepta")
    void whenRegister_withZeroReplacementValue_thenAccepts() {
        ToolGroupEntity group = buildGroup("X", "Y", 0.0, 100.0, 1);
        when(toolGroupRepository.save(any(ToolGroupEntity.class))).thenReturn(group);
        when(customerRepository.findByEmail("system@toolrent.com"))
                .thenReturn(Optional.of(buildCustomer()));

        ToolGroupEntity result = toolGroupService.registerToolGroup("X", "Y", 0.0, 100.0, 1);

        assertThat(result.getReplacementValue()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Registrar grupo: tarifa diaria negativa → se acepta (no hay validación)")
    void whenRegister_withNegativeDailyRate_thenAccepts() {
        ToolGroupEntity group = buildGroup("X", "Y", 1000.0, -500.0, 1);
        when(toolGroupRepository.save(any(ToolGroupEntity.class))).thenReturn(group);
        when(customerRepository.findByEmail("system@toolrent.com"))
                .thenReturn(Optional.of(buildCustomer()));

        ToolGroupEntity result = toolGroupService.registerToolGroup("X", "Y", 1000.0, -500.0, 1);

        assertThat(result.getTariff().getDailyRentalRate()).isEqualTo(-500.0);
    }

    @Test
    @DisplayName("Registrar grupo: nombre de 255 caracteres → OK")
    void whenRegister_withMaxLongName_thenOk() {
        String bigName = "X".repeat(255);
        ToolGroupEntity group = buildGroup(bigName, "Cat", 1000.0, 100.0, 1);
        when(toolGroupRepository.save(any(ToolGroupEntity.class))).thenReturn(group);
        when(customerRepository.findByEmail("system@toolrent.com"))
                .thenReturn(Optional.of(buildCustomer()));

        ToolGroupEntity result = toolGroupService.registerToolGroup(bigName, "Cat", 1000.0, 100.0, 1);

        assertThat(result.getName()).hasSize(255);
    }

    @Test
    @DisplayName("Registrar grupo: categoría null → excepción")
    void whenRegister_withNullCategory_thenThrows() {
        assertThatThrownBy(() -> toolGroupService.registerToolGroup("Name", null, 1000.0, 100.0, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @Test
    @DisplayName("Registrar grupo: stock 1 → crea 1 unidad")
    void whenRegister_withStockOne_thenOneUnit() {
        ToolGroupEntity group = buildGroup("A", "B", 1000.0, 100.0, 1);
        when(toolGroupRepository.save(any(ToolGroupEntity.class))).thenReturn(group);
        when(customerRepository.findByEmail("system@toolrent.com"))
                .thenReturn(Optional.of(buildCustomer()));

        ToolGroupEntity result = toolGroupService.registerToolGroup("A", "B", 1000.0, 100.0, 1);

        assertThat(result.getUnits()).hasSize(1);
        assertThat(result.getUnits().get(0).getStatus()).isEqualTo(ToolStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Registrar grupo: cliente sistema no existe → lo crea y usa")
    void whenRegister_systemCustomerNotFound_thenCreatesAndUses() {
        ToolGroupEntity group = buildGroup("A", "B", 1000.0, 100.0, 1);
        when(toolGroupRepository.save(any(ToolGroupEntity.class))).thenReturn(group);
        when(customerRepository.findByEmail("system@toolrent.com")).thenReturn(Optional.empty());
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(buildCustomer());

        ToolGroupEntity result = toolGroupService.registerToolGroup("A", "B", 1000.0, 100.0, 1);

        verify(customerRepository).save(any(CustomerEntity.class));
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    @DisplayName("Registrar grupo: tarifa fija 2500 → se asigna automáticamente")
    void whenRegister_tariffFixed2500_thenAssigned() {
        ToolGroupEntity group = buildGroup("A", "B", 1000.0, 100.0, 1);
        when(toolGroupRepository.save(any(ToolGroupEntity.class))).thenReturn(group);
        when(customerRepository.findByEmail("system@toolrent.com"))
                .thenReturn(Optional.of(buildCustomer()));

        ToolGroupEntity result = toolGroupService.registerToolGroup("A", "B", 1000.0, 100.0, 1);

        assertThat(result.getTariff().getDailyFineRate()).isEqualTo(2500.0);
    }

    /* ---------------------------------------------------------- */
    /* --------------------- LISTADOS --------------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Listar todos: repositorio vacío → iterable vacío")
    void whenGetAll_emptyRepo_thenReturnsEmpty() {
        when(toolGroupRepository.findAll()).thenReturn(java.util.List.of());

        Iterable<ToolGroupEntity> result = toolGroupService.getAllToolGroups();

        assertThat(result).isEmpty();
        verify(toolGroupRepository).findAll();
    }

    @Test
    @DisplayName("Listar todos: varios grupos → devuelve todos")
    void whenGetAll_multipleGroups_thenReturnsAll() {
        List<ToolGroupEntity> list = List.of(
                buildGroup("A", "X", 1000.0, 100.0, 1),
                buildGroup("B", "Y", 2000.0, 200.0, 2)
        );
        when(toolGroupRepository.findAll()).thenReturn(list);

        Iterable<ToolGroupEntity> result = toolGroupService.getAllToolGroups();

        assertThat(result).hasSize(2);
        verify(toolGroupRepository).findAll();
    }

    /* ---------------------------------------------------------- */
    /* --------------------- AUXILIARES ------------------------- */
    /* ---------------------------------------------------------- */

    private ToolGroupEntity buildGroup(String name, String category, double replacementValue, double dailyRate,
                                       int stock) {
        TariffEntity tariff = new TariffEntity();
        tariff.setDailyRentalRate(dailyRate);
        tariff.setDailyFineRate(2500.0);

        ToolGroupEntity g = new ToolGroupEntity();
        g.setId(1L);
        g.setName(name);
        g.setCategory(category);
        g.setReplacementValue(replacementValue);
        g.setTariff(tariff);

        for (int i = 0; i < stock; i++) {
            ToolUnitEntity unit = new ToolUnitEntity();
            unit.setId((long) (i + 1));
            unit.setStatus(ToolStatus.AVAILABLE);
            unit.setToolGroup(g);
            g.getUnits().add(unit);
        }
        return g;
    }

    private CustomerEntity buildCustomer() {
        CustomerEntity c = new CustomerEntity();
        c.setId(1L);
        c.setName("Sistema");
        c.setRut("0-0");
        c.setPhone("000");
        c.setEmail("system@toolrent.com");
        c.setStatus(CustomerStatus.ACTIVE);
        return c;
    }
}