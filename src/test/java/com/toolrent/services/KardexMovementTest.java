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

    /* ---------- GET ALL MOVEMENTS ---------- */

    @Test
    void whenGetAllMovements_withMixedData_thenReturnsListInOrder() {
        // Given
        ToolEntity tool = buildTool(1L);
        CustomerEntity customer = buildCustomer(10L);

        KardexMovementEntity m1 = buildKardexMovement(1L, tool, customer, MovementType.LOAN,
                LocalDateTime.now().minusDays(2), "Loan 2 units", 2);
        KardexMovementEntity m2 = buildKardexMovement(2L, tool, customer, MovementType.RETURN,
                LocalDateTime.now().minusDays(1), "Return 2 units", 2);
        KardexMovementEntity m3 = buildKardexMovement(3L, tool, customer, MovementType.RETIRE,
                LocalDateTime.now(), "Retire broken", 1);

        when(kardexMovementRepository.findAll()).thenReturn(List.of(m1, m2, m3));

        // When
        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        // Then
        assertThat(result)
                .hasSize(3)
                .containsExactly(m1, m2, m3);
        verify(kardexMovementRepository).findAll();
    }

    @Test
    void whenGetAllMovements_withEmptyData_thenReturnsEmptyIterable() {
        // Given
        when(kardexMovementRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        // Then
        assertThat(result).isEmpty();
        verify(kardexMovementRepository).findAll();
    }

    @Test
    void whenGetAllMovements_withNullElements_thenHandlesGracefully() {
        // Given
        // Arrays.asList SÍ permite nulls
        when(kardexMovementRepository.findAll()).thenReturn(Arrays.asList(null, null));

        // When
        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        // Then
        assertThat(result).hasSize(2).containsOnlyNulls();
        verify(kardexMovementRepository).findAll();
    }

    @Test
    void whenGetAllMovements_withLargeDataset_thenDoesNotFail() {
        // Given
        ToolEntity tool = buildTool(1L);
        CustomerEntity customer = buildCustomer(10L);

        List<KardexMovementEntity> bigList = java.util.stream.IntStream
                .rangeClosed(1, 10_000)
                .mapToObj(i -> buildKardexMovement((long) i, tool, customer,
                        i % 2 == 0 ? MovementType.LOAN : MovementType.RETURN,
                        LocalDateTime.now().minusHours(i), "Auto-generated", 1))
                .toList();

        when(kardexMovementRepository.findAll()).thenReturn(bigList);

        // When
        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        // Then
        assertThat(result).hasSize(10_000);
        verify(kardexMovementRepository).findAll();
    }

    /* ---------- EDGE CASES ---------- */

    @Test
    void whenGetAllMovements_withAllMovementTypes_thenCoverageComplete() {
        // Given
        ToolEntity tool = buildTool(1L);
        CustomerEntity customer = buildCustomer(10L);

        KardexMovementEntity reg = buildKardexMovement(1L, tool, customer, MovementType.REGISTRY,
                LocalDateTime.now().minusDays(4), "Initial registry", 10);
        KardexMovementEntity loan = buildKardexMovement(2L, tool, customer, MovementType.LOAN,
                LocalDateTime.now().minusDays(3), "Loan 3 units", 3);
        KardexMovementEntity ret = buildKardexMovement(3L, tool, customer, MovementType.RETURN,
                LocalDateTime.now().minusDays(2), "Return 3 units", 3);
        KardexMovementEntity retire = buildKardexMovement(4L, tool, customer, MovementType.RETIRE,
                LocalDateTime.now().minusDays(1), "Retire damaged", 1);
        KardexMovementEntity repair = buildKardexMovement(5L, tool, customer, MovementType.REPAIR,
                LocalDateTime.now(), "Sent to repair", 1);

        when(kardexMovementRepository.findAll())
                .thenReturn(List.of(reg, loan, ret, retire, repair));

        // When
        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        // Then
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
    void whenGetAllMovements_withNullDetails_thenAcceptsNull() {
        // Given
        ToolEntity tool = buildTool(1L);
        CustomerEntity customer = buildCustomer(10L);
        KardexMovementEntity m = buildKardexMovement(1L, tool, customer, MovementType.REPAIR,
                LocalDateTime.now(), null, 1);

        when(kardexMovementRepository.findAll()).thenReturn(List.of(m));

        // When
        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getDetails()).isNull();
    }

    @Test
    void whenGetAllMovements_withNegativeQuantity_thenAllowed() {
        // Given
        ToolEntity tool = buildTool(1L);
        CustomerEntity customer = buildCustomer(10L);
        KardexMovementEntity m = buildKardexMovement(1L, tool, customer, MovementType.RETIRE,
                LocalDateTime.now(), "Remove defective", -5);

        when(kardexMovementRepository.findAll()).thenReturn(List.of(m));

        // When
        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getQuantity()).isEqualTo(-5);
    }

    /* ---------- HELPERS ---------- */
    private ToolEntity buildTool(Long id) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName("Tool-" + id);
        return t;
    }

    private CustomerEntity buildCustomer(Long id) {
        CustomerEntity c = new CustomerEntity();
        c.setId(id);
        c.setName("Customer-" + id);
        return c;
    }

    private KardexMovementEntity buildKardexMovement(Long id,
                                                     ToolEntity tool,
                                                     CustomerEntity customer,
                                                     MovementType type,
                                                     LocalDateTime date,
                                                     String details,
                                                     Integer quantity) {
        KardexMovementEntity k = new KardexMovementEntity();
        k.setId(id);
        k.setTool(tool);
        k.setCustomer(customer);
        k.setMovementType(type);
        k.setMovementDate(date);
        k.setDetails(details);
        k.setQuantity(quantity);
        return k;
    }
}