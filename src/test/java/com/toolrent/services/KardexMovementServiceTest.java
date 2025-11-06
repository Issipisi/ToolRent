package com.toolrent.services;

import com.toolrent.config.SecurityConfig;
import com.toolrent.entities.*;
import com.toolrent.repositories.KardexMovementRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KardexMovementServiceTest {

    @Mock private KardexMovementRepository kardexMovementRepository;
    @Mock private CustomerService customerService;
    @InjectMocks private KardexMovementService kardexService;

    /* ======================================================================
            1. getAllMovements(lista vacía / con datos)
       ====================================================================== */

    @Test @DisplayName("getAllMovements – con datos")
    void getAll_withData(){
        KardexMovementEntity m1 = buildMovement(1L, MovementType.LOAN);
        KardexMovementEntity m2 = buildMovement(2L, MovementType.RETURN);
        when(kardexMovementRepository.findAllWithDetails()).thenReturn(List.of(m1,m2));

        List<KardexMovementEntity> res = kardexService.getAllMovements();

        assertThat(res).hasSize(2);
        verify(kardexMovementRepository).findAllWithDetails();
    }

    @Test @DisplayName("getAllMovements – vacía")
    void getAll_empty(){
        when(kardexMovementRepository.findAllWithDetails()).thenReturn(List.of());

        List<KardexMovementEntity> res = kardexService.getAllMovements();

        assertThat(res).isEmpty();
    }

    /* ======================================================================
              2. findByToolGroupId(vacía / con datos)
       ====================================================================== */

    @Test @DisplayName("findByToolGroupId – con datos")
    void byToolGroup_withData(){
        KardexMovementEntity m = buildMovement(3L, MovementType.REGISTRY);
        when(kardexMovementRepository.findByToolGroupId(10L)).thenReturn(List.of(m));

        List<KardexMovementEntity> res = kardexService.findByToolGroupId(10L);

        assertThat(res).hasSize(1);
        verify(kardexMovementRepository).findByToolGroupId(10L);
    }

    @Test @DisplayName("findByToolGroupId – vacía")
    void byToolGroup_empty(){
        when(kardexMovementRepository.findByToolGroupId(99L)).thenReturn(List.of());

        List<KardexMovementEntity> res = kardexService.findByToolGroupId(99L);

        assertThat(res).isEmpty();
    }

    /* ======================================================================
              3. findByDateRange(invertido / normal / vacío)
       ====================================================================== */

    @Test @DisplayName("findByDateRange – rango válido")
    void dateRange_ok(){
        LocalDateTime from = LocalDateTime.of(2025,1,1,0,0);
        LocalDateTime to   = LocalDateTime.of(2025,1,31,23,59);
        KardexMovementEntity m = buildMovement(4L, MovementType.RETIRE);
        when(kardexMovementRepository.findByDateRange(from,to)).thenReturn(List.of(m));

        List<KardexMovementEntity> res = kardexService.findByDateRange(from,to);

        assertThat(res).hasSize(1);
    }

    @Test @DisplayName("findByDateRange – vacío")
    void dateRange_empty(){
        LocalDateTime from = LocalDateTime.of(2025,2,1,0,0);
        LocalDateTime to   = LocalDateTime.of(2025,2,1,0,0);
        when(kardexMovementRepository.findByDateRange(from,to)).thenReturn(List.of());

        List<KardexMovementEntity> res = kardexService.findByDateRange(from,to);

        assertThat(res).isEmpty();
    }

    /* ======================================================================
              4. saveRegistryKardex – 100 % branch  (stock == 0  /  >0)
       ====================================================================== */

    @Test @DisplayName("saveRegistryKardex – stock 0 → no hace nada")
    void registry_stockZero(){
        // Solo necesitamos un grupo cualquiera; no hace falta mockear getUnits/getName
        ToolGroupEntity group = mock(ToolGroupEntity.class);

        kardexService.saveRegistryKardex(group, 0);

        verifyNoInteractions(kardexMovementRepository);
    }

    @Test @DisplayName("saveRegistryKardex – stock >0 → guarda movimiento")
    void registry_positiveStock(){
        try (MockedStatic<SecurityConfig> mocked = mockStatic(SecurityConfig.class)) {
            mocked.when(SecurityConfig::getCurrentUsername).thenReturn("admin");

            ToolGroupEntity group = mockGroupWithUnits();
            CustomerEntity systemCustomer = new CustomerEntity();
            systemCustomer.setName("Sistema");
            when(customerService.getSystemCustomer()).thenReturn(systemCustomer);

            KardexMovementEntity saved = new KardexMovementEntity();
            saved.setId(100L);
            when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenReturn(saved);

            kardexService.saveRegistryKardex(group, 5);

            ArgumentCaptor<KardexMovementEntity> captor =
                    ArgumentCaptor.forClass(KardexMovementEntity.class);
            verify(kardexMovementRepository).save(captor.capture());

            KardexMovementEntity captured = captor.getValue();
            assertThat(captured.getMovementType()).isEqualTo(MovementType.REGISTRY);
            assertThat(captured.getCustomer()).isSameAs(systemCustomer);
            assertThat(captured.getDetails()).contains("Creación de grupo")
                    .contains("Stock inicial: 5")
                    .contains("Usuario: admin");
        }
    }

    /* ======================================================================
                                  Helpers                                                               /
       ====================================================================== */

    private KardexMovementEntity buildMovement(Long id, MovementType type){
        KardexMovementEntity m = new KardexMovementEntity();
        m.setId(id);
        m.setMovementType(type);
        return m;
    }

    private ToolGroupEntity mockGroupWithUnits(){
        ToolGroupEntity g = mock(ToolGroupEntity.class);
        List<ToolUnitEntity> list = List.of(mock(ToolUnitEntity.class));
        when(g.getUnits()).thenReturn(list);
        when(g.getName()).thenReturn("Taladro");
        return g;
    }
}