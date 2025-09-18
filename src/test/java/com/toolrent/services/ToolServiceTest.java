package com.toolrent.services;

import com.toolrent.entities.*;
import com.toolrent.repositories.CustomerRepository;
import com.toolrent.repositories.KardexMovementRepository;
import com.toolrent.repositories.TariffRepository;
import com.toolrent.repositories.ToolRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private KardexMovementRepository kardexMovementRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TariffRepository tariffRepository;

    @InjectMocks
    private ToolService toolService;

    private CustomerEntity systemCustomer;

    @BeforeEach
    void setUp() {
        systemCustomer = buildSystemCustomer();
    }

    /* ---------------------------------------------------------- */
    /* --------------------- REGISTRAR HERRAMIENTA -------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Registrar herramienta: datos válidos → crea, asigna tarifa y genera kardex")
    void whenRegisterTool_withValidData_thenSuccess() {
        ToolEntity saved = buildTool(1L, "Taladro", "Eléctrica", 150.0, 15.0, 5, ToolStatus.AVAILABLE);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(saved);
        when(customerRepository.findByEmail("system@toolrent.com")).thenReturn(Optional.of(systemCustomer));

        ToolEntity result = toolService.registerTool("Taladro", "Eléctrica", 150.0, 15.0, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Taladro");
        assertThat(result.getStock()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo(ToolStatus.AVAILABLE);

        verify(toolRepository).save(any(ToolEntity.class));
        verifyNoInteractions(tariffRepository); // cascada lo hace sin llamar al mock
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
        verify(customerRepository).findByEmail("system@toolrent.com");
    }

    @Test
    @DisplayName("Registrar herramienta: stock negativo → lanza excepción")
    void whenRegisterTool_withNegativeStock_thenThrows() {
        assertThatThrownBy(() -> toolService.registerTool("A", "B", 10.0, 1.0, -5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El stock no puede ser negativo");

        verifyNoInteractions(toolRepository, customerRepository, tariffRepository, kardexMovementRepository);
    }

    @Test
    @DisplayName("Registrar herramienta: stock cero → guarda correctamente")
    void whenRegisterTool_withZeroStock_thenStillSaves() {
        ToolEntity saved = buildTool(1L, "A", "B", 10.0, 1.0, 0, ToolStatus.AVAILABLE);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(saved);
        when(customerRepository.findByEmail("system@toolrent.com")).thenReturn(Optional.of(systemCustomer));

        ToolEntity result = toolService.registerTool("A", "B", 10.0, 1.0, 0);

        assertThat(result.getStock()).isZero();
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @ParameterizedTest(name = "name=''{0}'' → debe fallar")
    @NullSource
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("Registrar herramienta: nombre nulo/vacío → lanza excepción")
    void whenRegisterTool_withNullOrBlankName_thenThrows(String name) {
        assertThatThrownBy(() -> toolService.registerTool(name, "cat", 100.0, 10.0, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
        verifyNoInteractions(toolRepository, customerRepository, tariffRepository, kardexMovementRepository);
    }

    @Test
    @DisplayName("Registrar herramienta: 100 threads concurrentes – sin excepciones")
    void whenRegisterTool_concurrent_thenNoException() throws InterruptedException {
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicReference<Exception> error = new AtomicReference<>();

        ToolEntity saved = buildTool(1L, "T", "C", 100.0, 10.0, 5, ToolStatus.AVAILABLE);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(saved);
        when(customerRepository.findByEmail("system@toolrent.com")).thenReturn(Optional.of(systemCustomer));

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    toolService.registerTool("T", "C", 100.0, 10.0, 5);
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        assertThat(error.get()).isNull();
        verify(toolRepository, times(threads)).save(any(ToolEntity.class));
    }


    /* ---------------------------------------------------------- */
    /* --------------------- CAMBIAR ESTADO --------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Cambiar estado: AVAILABLE → LOANED sin tocar stock")
    void whenChangeStatus_toLoaned_thenSuccess() {
        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 2, ToolStatus.AVAILABLE);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(tool)).thenReturn(tool);

        ToolEntity result = toolService.changeStatus(1L, ToolStatus.LOANED);

        assertThat(result.getStatus()).isEqualTo(ToolStatus.LOANED);
        assertThat(result.getStock()).isEqualTo(2);
        verify(toolRepository).save(tool);
        verifyNoInteractions(kardexMovementRepository, customerRepository);
    }

    @Test
    @DisplayName("Cambiar estado: LOANED → AVAILABLE incrementa stock en 1")
    void whenChangeStatusToAvailable_thenIncrementStock() {
        ToolEntity tool = buildTool(1L, "T", "C", 100.0, 10.0, 3, ToolStatus.LOANED);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(tool)).thenReturn(tool);

        ToolEntity result = toolService.changeStatus(1L, ToolStatus.AVAILABLE);

        assertThat(result.getStock()).isEqualTo(4);
        assertThat(result.getStatus()).isEqualTo(ToolStatus.AVAILABLE);
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("Cambiar estado: LOANED → IN_REPAIR (sin tocar stock)")
    void whenChangeStatus_LoanedToInRepair_thenNoStockChange() {
        ToolEntity tool = buildTool(1L, "T", "C", 100.0, 10.0, 3, ToolStatus.LOANED);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(tool)).thenReturn(tool);

        ToolEntity result = toolService.changeStatus(1L, ToolStatus.IN_REPAIR);

        assertThat(result.getStatus()).isEqualTo(ToolStatus.IN_REPAIR);
        assertThat(result.getStock()).isEqualTo(3); // sin cambio
        verify(toolRepository).save(tool);
    }

    @Test
    @DisplayName("Cambiar estado: tool not found → excepción")
    void whenChangeStatus_toolNotFound_thenThrows() {
        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolService.changeStatus(999L, ToolStatus.AVAILABLE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tool not found");

        verify(toolRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambiar estado: mismo estado → lanza excepción")
    void whenChangeStatus_sameStatus_thenThrows() {
        ToolEntity tool = buildTool(1L, "T", "C", 100.0, 10.0, 3, ToolStatus.AVAILABLE);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));

        assertThatThrownBy(() -> toolService.changeStatus(1L, ToolStatus.AVAILABLE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("La herramienta ya se encuentra en estado: AVAILABLE");

        verify(toolRepository, never()).save(any());
        verifyNoInteractions(customerRepository);
    }


    /* ---------- UPDATE REPLACEMENT VALUE – COBERTURA FALTANTE ---------- */

    @Test
    @DisplayName("updateReplacementValue: valor null → excepción")
    void whenUpdateReplacementValue_withNull_thenThrows() {
        ToolEntity tool = buildTool(1L, "T", "C", 100.0, 10.0, 3, ToolStatus.AVAILABLE);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));

        assertThatThrownBy(() -> toolService.updateReplacementValue(1L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El valor de reposición debe ser mayor a cero");

        verify(toolRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReplacementValue: valor cero → excepción")
    void whenUpdateReplacementValue_withZero_thenThrows() {
        ToolEntity tool = buildTool(1L, "T", "C", 100.0, 10.0, 3, ToolStatus.AVAILABLE);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));

        assertThatThrownBy(() -> toolService.updateReplacementValue(1L, 0.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El valor de reposición debe ser mayor a cero");

        verify(toolRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReplacementValue: tool not found → excepción")
    void whenUpdateReplacementValue_toolNotFound_thenThrows() {
        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolService.updateReplacementValue(999L, 150.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tool not found");

        verify(toolRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReplacementValue: valor válido → actualiza y devuelve")
    void whenUpdateReplacementValue_withValidValue_thenUpdates() {
        ToolEntity tool = buildTool(1L, "T", "C", 100.0, 10.0, 3, ToolStatus.AVAILABLE);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(tool)).thenReturn(tool);

        ToolEntity result = toolService.updateReplacementValue(1L, 150.0);

        assertThat(result.getReplacementValue()).isEqualTo(150.0);
        verify(toolRepository).save(tool);
    }

    /* ---------------------------------------------------------- */
    /* --------------------- MODIFICAR STOCK -------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Actualizar stock: valor final positivo → ok")
    void whenUpdateStock_withPositiveDelta_thenIncreases() {
        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 5, ToolStatus.AVAILABLE);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(tool)).thenReturn(tool);

        ToolEntity result = toolService.updateStock(1L, 8);

        assertThat(result.getStock()).isEqualTo(8);
        verify(toolRepository).save(tool);
        verifyNoInteractions(kardexMovementRepository, customerRepository);
    }

    @Test
    @DisplayName("updateStock: tool not found → excepción")
    void whenUpdateStock_toolNotFound_thenThrows() {
        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolService.updateStock(999L, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tool not found");

        verify(toolRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar stock: resultado negativo → excepción")
    void whenUpdateStock_withNegativeResult_thenThrows() {
        ToolEntity tool = buildTool(1L, "A", "C", 100.0, 10.0, 2, ToolStatus.AVAILABLE);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));

        assertThatThrownBy(() -> toolService.updateStock(1L, -5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock resultante no puede ser negativo");

        verify(toolRepository, never()).save(any());
        verifyNoInteractions(customerRepository);
    }

    /* ---------------------------------------------------------- */
    /* --------------------- DAR DE BAJA ------------------------ */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Desactivar herramienta: existente → estado RETIRED y kardex")
    void whenDesactivateTool_withExistingId_thenSetsStatusRetired() {
        Long id = 1L;
        ToolEntity tool = buildTool(id, "T", "C", 100.0, 10.0, 1, ToolStatus.AVAILABLE);
        when(toolRepository.findById(id)).thenReturn(Optional.of(tool));
        when(customerRepository.findByEmail("system@toolrent.com")).thenReturn(Optional.of(systemCustomer));

        toolService.desactivateTool(id);

        assertThat(tool.getStatus()).isEqualTo(ToolStatus.RETIRED);
        verify(toolRepository).save(tool);
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
        verify(customerRepository).findByEmail("system@toolrent.com");
    }

    /* ---------------------------------------------------------- */
    /* --------------------- LISTAR  ---------------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Listar todas las herramientas → devuelve lista completa")
    void whenGetAllTools_thenReturnsList() {
        List<ToolEntity> data = List.of(
                buildTool(1L, "A", "C1", 10.0, 1.0, 5, ToolStatus.AVAILABLE),
                buildTool(2L, "B", "C2", 20.0, 2.0, 0, ToolStatus.RETIRED)
        );
        when(toolRepository.findAll()).thenReturn(data);

        Iterable<ToolEntity> result = toolService.getAllTools();

        assertThat(result).hasSize(2);
        verify(toolRepository).findAll();
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("getAllTools: lista vacía → devuelve iterable vacío")
    void whenGetAllTools_empty_thenEmptyIterable() {
        when(toolRepository.findAll()).thenReturn(List.of());

        Iterable<ToolEntity> result = toolService.getAllTools();

        assertThat(result).isEmpty();
        verify(toolRepository).findAll();
    }

    @Test
    @DisplayName("getAllTools: lista con nulls → devuelve iterable con nulls")
    void whenGetAllTools_withNulls_thenReturnsIterableWithNulls() {
        // Arrays.asList SÍ permite nulls
        when(toolRepository.findAll()).thenReturn(Arrays.asList(null, null));

        Iterable<ToolEntity> result = toolService.getAllTools();

        assertThat(result).hasSize(2).containsOnlyNulls();
        verify(toolRepository).findAll();
    }

    /* ---------------------------------------------------------- */
    /* --------------------- AUXILIARES ------------------------- */
    /* ---------------------------------------------------------- */

    private ToolEntity buildTool(Long id, String name, String category,
                                 Double replacementValue, Double pricePerDay,
                                 int stock, ToolStatus status) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName(name);
        t.setCategory(category);
        t.setReplacementValue(replacementValue);
        t.setPricePerDay(pricePerDay);
        t.setStock(stock);
        t.setStatus(status);
        return t;
    }

    private CustomerEntity buildSystemCustomer() {
        CustomerEntity c = new CustomerEntity();
        c.setId(0L);
        c.setName("SYSTEM");
        c.setEmail("system@toolrent.com");
        c.setRut("0");
        c.setPhone("0");
        c.setStatus(CustomerStatus.ACTIVE);
        return c;
    }
}