package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.KardexMovementRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KardexMovementServiceTest {

    @Mock
    private KardexMovementRepository kardexMovementRepository;

    @InjectMocks
    private KardexMovementService kardexMovementService;

    /* ---------------------------------------------------------- */
    /* --------------------- CATÁLOGO COMPLETO ------------------ */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Consultar todos: lista mezclada → devuelve elementos en orden del repositorio")
    void whenGetAllMovements_withMixedData_thenReturnsListInOrder() {
        ToolEntity tool = buildTool();
        CustomerEntity customer = buildCustomer();

        KardexMovementEntity m1 = buildKardexMovement(1L, tool, customer, MovementType.LOAN,
                LocalDateTime.now().minusDays(2), "Loan 2 units");
        KardexMovementEntity m2 = buildKardexMovement(2L, tool, customer, MovementType.RETURN,
                LocalDateTime.now().minusDays(1), "Return 2 units");
        KardexMovementEntity m3 = buildKardexMovement(3L, tool, customer, MovementType.RETIRE,
                LocalDateTime.now(), "Retire broken");

        when(kardexMovementRepository.findAll()).thenReturn(List.of(m1, m2, m3));

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result).hasSize(3).containsExactly(m1, m2, m3);
        verify(kardexMovementRepository).findAll();
    }

    @Test
    @DisplayName("Consultar todos: sin datos → iterable vacío")
    void whenGetAllMovements_withEmptyData_thenReturnsEmptyIterable() {
        when(kardexMovementRepository.findAll()).thenReturn(Collections.emptyList());

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result).isEmpty();
        verify(kardexMovementRepository).findAll();
    }

    @Test
    @DisplayName("Consultar todos: lista con nulls → maneja sin excepción")
    void whenGetAllMovements_withNullElements_thenHandlesGracefully() {
        when(kardexMovementRepository.findAll()).thenReturn(Arrays.asList(null, null));

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result).hasSize(2).containsOnlyNulls();
        verify(kardexMovementRepository).findAll();
    }

    @Test
    @DisplayName("Consultar todos: 10 000 registros → no falla y mantiene tamaño")
    void whenGetAllMovements_withLargeDataset_thenDoesNotFail() {
        ToolEntity tool = buildTool();
        CustomerEntity customer = buildCustomer();

        List<KardexMovementEntity> bigList = java.util.stream.IntStream
                .rangeClosed(1, 10_000)
                .mapToObj(i -> buildKardexMovement((long) i, tool, customer,
                        i % 2 == 0 ? MovementType.LOAN : MovementType.RETURN,
                        LocalDateTime.now().minusHours(i), "Auto-generated"))
                .toList();

        when(kardexMovementRepository.findAll()).thenReturn(bigList);

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result).hasSize(10_000);
        verify(kardexMovementRepository).findAll();
    }

    /* ---------------------------------------------------------- */
    /* --------------------- CASOS EDGE / RAROS ----------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Consultar todos: cubre todos los tipos de movimiento")
    void whenGetAllMovements_withAllMovementTypes_thenCoverageComplete() {
        ToolEntity tool = buildTool();
        CustomerEntity customer = buildCustomer();

        KardexMovementEntity reg = buildKardexMovement(1L, tool, customer, MovementType.REGISTRY,
                LocalDateTime.now().minusDays(4), "Initial registry");
        KardexMovementEntity loan = buildKardexMovement(2L, tool, customer, MovementType.LOAN,
                LocalDateTime.now().minusDays(3), "Loan 3 units");
        KardexMovementEntity ret = buildKardexMovement(3L, tool, customer, MovementType.RETURN,
                LocalDateTime.now().minusDays(2), "Return 3 units");
        KardexMovementEntity retire = buildKardexMovement(4L, tool, customer, MovementType.RETIRE,
                LocalDateTime.now().minusDays(1), "Retire damaged");
        KardexMovementEntity repair = buildKardexMovement(5L, tool, customer, MovementType.REPAIR,
                LocalDateTime.now(), "Sent to repair");

        when(kardexMovementRepository.findAll())
                .thenReturn(List.of(reg, loan, ret, retire, repair));

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result)
                .hasSize(5)
                .extracting(KardexMovementEntity::getMovementType)
                .containsExactly(MovementType.REGISTRY,
                        MovementType.LOAN,
                        MovementType.RETURN,
                        MovementType.RETIRE,
                        MovementType.REPAIR);
    }

    @Test
    @DisplayName("Consultar todos: details null → acepta sin problema")
    void whenGetAllMovements_withNullDetails_thenAcceptsNull() {
        ToolEntity tool = buildTool();
        CustomerEntity customer = buildCustomer();
        KardexMovementEntity m = buildKardexMovement(1L, tool, customer, MovementType.REPAIR,
                LocalDateTime.now(), null);

        when(kardexMovementRepository.findAll()).thenReturn(List.of(m));

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getDetails()).isNull();
    }

    @Test
    @DisplayName("Consultar todos: cantidad negativa en lógica → permitido (no valida negativos)")
    void whenGetAllMovements_withNegativeQuantity_thenAllowed() {
        ToolEntity tool = buildTool();
        CustomerEntity customer = buildCustomer();
        KardexMovementEntity m = buildKardexMovement(1L, tool, customer, MovementType.RETIRE,
                LocalDateTime.now(), "Remove defective");

        when(kardexMovementRepository.findAll()).thenReturn(List.of(m));

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result).hasSize(1);
    }

    /* ---------------------------------------------------------- */
    /* --------------------- HELPERS ---------------------------- */
    /* ---------------------------------------------------------- */

    private ToolEntity buildTool() {
        ToolEntity t = new ToolEntity();
        t.setId(1L);
        t.setName("Tool-1");
        return t;
    }

    private CustomerEntity buildCustomer() {
        CustomerEntity c = new CustomerEntity();
        c.setId(10L);
        c.setName("Customer-10");
        return c;
    }

    private KardexMovementEntity buildKardexMovement(Long id,
                                                     ToolEntity tool,
                                                     CustomerEntity customer,
                                                     MovementType type,
                                                     LocalDateTime date,
                                                     String details) {
        KardexMovementEntity k = new KardexMovementEntity();
        k.setId(id);
        k.setTool(tool);
        k.setCustomer(customer);
        k.setMovementType(type);
        k.setMovementDate(date);
        k.setDetails(details);
        return k;
    }
}