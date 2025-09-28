package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.KardexMovementRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

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
    @DisplayName("10 000 registros → no falla y mantiene tamaño")
    void whenGetAllMovements_withLargeDataset_thenDoesNotFail() {
        ToolGroupEntity group = buildToolGroup(1L, "X", "Y", 1000.0, 100.0);
        ToolUnitEntity unit = group.getUnits().get(0);
        CustomerEntity customer = buildCustomer(1L, "Sys", "0-0", "000", "sys@test.com");

        List<KardexMovementEntity> bigList = IntStream.rangeClosed(1, 10_000)
                .mapToObj(i -> buildKardexMovement((long) i, unit, customer,
                        i % 2 == 0 ? MovementType.LOAN : MovementType.RETURN,
                        LocalDateTime.now().minusHours(i), "Auto-generated"))
                .toList();

        when(kardexMovementRepository.findAll()).thenReturn(bigList);

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result).hasSize(10_000);
        verify(kardexMovementRepository).findAll();
    }

    @Test
    @DisplayName("Details null → acepta sin problema")
    void whenGetAllMovements_withNullDetails_thenAcceptsNull() {
        ToolGroupEntity group = buildToolGroup(1L, "X", "Y", 1000.0, 100.0);
        ToolUnitEntity unit = group.getUnits().get(0);
        CustomerEntity customer = buildCustomer(1L, "Sys", "0-0", "000", "sys@test.com");

        KardexMovementEntity m = buildKardexMovement(1L, unit, customer, MovementType.REPAIR,
                LocalDateTime.now(), null);

        when(kardexMovementRepository.findAll()).thenReturn(List.of(m));

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getDetails()).isNull();
    }

    @Test
    @DisplayName("Cubre todos los tipos de movimiento")
    void whenGetAllMovements_withAllMovementTypes_thenCoverageComplete() {
        ToolGroupEntity group = buildToolGroup(1L, "X", "Y", 1000.0, 100.0);
        ToolUnitEntity unit = group.getUnits().get(0);
        CustomerEntity customer = buildCustomer(1L, "Sys", "0-0", "000", "sys@test.com");

        KardexMovementEntity m1 = buildKardexMovement(1L, unit, customer, MovementType.REGISTRY,
                LocalDateTime.now().minusDays(4), "Initial registry");
        KardexMovementEntity m2 = buildKardexMovement(2L, unit, customer, MovementType.LOAN,
                LocalDateTime.now().minusDays(3), "Loan 3 units");
        KardexMovementEntity m3 = buildKardexMovement(3L, unit, customer, MovementType.RETURN,
                LocalDateTime.now().minusDays(2), "Return 3 units");
        KardexMovementEntity m4 = buildKardexMovement(4L, unit, customer, MovementType.RETIRE,
                LocalDateTime.now().minusDays(1), "Retire damaged");
        KardexMovementEntity m5 = buildKardexMovement(5L, unit, customer, MovementType.REPAIR,
                LocalDateTime.now(), "Sent to repair");

        when(kardexMovementRepository.findAll()).thenReturn(List.of(m1, m2, m3, m4, m5));

        Iterable<KardexMovementEntity> result = kardexMovementService.getAllMovements();

        assertThat(result)
                .hasSize(5)
                .extracting(KardexMovementEntity::getMovementType)
                .containsExactly(MovementType.REGISTRY, MovementType.LOAN, MovementType.RETURN,
                        MovementType.RETIRE, MovementType.REPAIR);
    }

    /* ---------------------------------------------------------- */
    /* --------------------- HELPERS ---------------------------- */
    /* ---------------------------------------------------------- */

    private ToolGroupEntity buildToolGroup(Long id, String name, String category, double replacementValue, double dailyRate) {
        ToolGroupEntity group = new ToolGroupEntity();
        group.setId(id);
        group.setName(name);
        group.setCategory(category);
        group.setReplacementValue(replacementValue);

        TariffEntity tariff = new TariffEntity();
        tariff.setDailyRentalRate(dailyRate);
        tariff.setDailyFineRate(dailyRate * 2);
        group.setTariff(tariff);

        for (int i = 0; i < 3; i++) {
            ToolUnitEntity unit = new ToolUnitEntity();
            unit.setId((long) (i + 1));
            unit.setStatus(ToolStatus.AVAILABLE);
            unit.setToolGroup(group);
            group.getUnits().add(unit);
        }
        return group;
    }

    private CustomerEntity buildCustomer(Long id, String name, String rut, String phone, String email) {
        CustomerEntity c = new CustomerEntity();
        c.setId(id);
        c.setName(name);
        c.setRut(rut);
        c.setPhone(phone);
        c.setEmail(email);
        c.setStatus(CustomerStatus.ACTIVE);
        return c;
    }

    private KardexMovementEntity buildKardexMovement(Long id, ToolUnitEntity unit, CustomerEntity customer,
                                                     MovementType type, LocalDateTime date, String details) {
        KardexMovementEntity k = new KardexMovementEntity();
        k.setId(id);
        k.setToolUnit(unit);
        k.setCustomer(customer);
        k.setMovementType(type);
        k.setMovementDate(date);
        k.setDetails(details);
        return k;
    }
}