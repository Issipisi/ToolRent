package com.toolrent.services;


import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KardexMovementServiceTest {

    /*@Mock
    private KardexMovementRepository kardexMovementRepository;

    @InjectMocks
    private KardexMovementService kardexMovementService;

    /* ---------------------------------------------------------- */
    /* --------------------- CATÁLOGO COMPLETO ------------------ */
    /* ---------------------------------------------------------- */

    /*@Test
    @DisplayName("10 000 registros → no falla y mantiene tamaño")
    void whenGetAllMovements_withLargeDataset_thenDoesNotFail() {
        ToolGroupEntity group = buildToolGroup();
        ToolUnitEntity unit = group.getUnits().get(0);
        CustomerEntity customer = buildCustomer();

        List<KardexMovementDTO> bigList = IntStream.rangeClosed(1, 10_000)
                .mapToObj(i -> buildKardexDTO((long) i,
                        i % 2 == 0 ? MovementType.LOAN : MovementType.RETURN,
                        "Auto-generated"))
                .toList();

        when(kardexMovementRepository.findAllProjected()).thenReturn(bigList);

        List<KardexMovementDTO> result = kardexMovementService.getAllMovements();

        assertThat(result).hasSize(10_000);
        verify(kardexMovementRepository).findAllProjected();
    }

    @Test
    @DisplayName("Details null → acepta sin problema")
    void whenGetAllMovements_withNullDetails_thenAcceptsNull() {
        KardexMovementDTO dto = buildKardexDTO(1L, MovementType.REPAIR, null);

        when(kardexMovementRepository.findAllProjected()).thenReturn(List.of(dto));

        List<KardexMovementDTO> result = kardexMovementService.getAllMovements();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).details()).isNull();
    }

    @Test
    @DisplayName("Cubre todos los tipos de movimiento")
    void whenGetAllMovements_withAllMovementTypes_thenCoverageComplete() {
        List<KardexMovementDTO> list = List.of(
                buildKardexDTO(1L, MovementType.REGISTRY, "Initial registry"),
                buildKardexDTO(2L, MovementType.LOAN, "Loan 3 units"),
                buildKardexDTO(3L, MovementType.RETURN, "Return 3 units"),
                buildKardexDTO(4L, MovementType.RETIRE, "Retire damaged"),
                buildKardexDTO(5L, MovementType.REPAIR, "Sent to repair")
        );

        when(kardexMovementRepository.findAllProjected()).thenReturn(list);

        List<KardexMovementDTO> result = kardexMovementService.getAllMovements();

        assertThat(result)
                .hasSize(5)
                .extracting(KardexMovementDTO::movementType)
                .containsExactly(MovementType.REGISTRY, MovementType.LOAN,
                        MovementType.RETURN, MovementType.RETIRE, MovementType.REPAIR);
    }

    /* ---------------------------------------------------------- */
    /* --------------------- HELPERS ---------------------------- */
    /* ---------------------------------------------------------- */

    /*private ToolGroupEntity buildToolGroup() {
        ToolGroupEntity group = new ToolGroupEntity();
        group.setId(1L);
        group.setName("X");
        group.setCategory("Y");
        group.setReplacementValue(1000.0);

        TariffEntity tariff = new TariffEntity();
        tariff.setDailyRentalRate(100.0);
        tariff.setDailyFineRate(100.0 * 2);
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

    private CustomerEntity buildCustomer() {
        CustomerEntity c = new CustomerEntity();
        c.setId(1L);
        c.setName("Sys");
        c.setRut("0-0");
        c.setPhone("000");
        c.setEmail("sys@test.com");
        c.setStatus(CustomerStatus.ACTIVE);
        return c;
    }

    private KardexMovementDTO buildKardexDTO(Long id, MovementType type, String details) {
        return new KardexMovementDTO(
                id,
                "2025-09-29 12:00",   // fecha simulada
                type,
                "Taladro",            // toolName
                "Cliente Genérico",   // customerName
                details);
    }*/
}