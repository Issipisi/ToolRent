package com.toolrent.services;

import com.toolrent.entities.TariffEntity;
import com.toolrent.repositories.TariffRepository;
import org.junit.jupiter.api.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @InjectMocks
    private TariffService tariffService;

    /* ---------- UPDATE EDGE ---------- */

    @Test
    @DisplayName("updateTariff: rates negativos – se aceptan")
    void whenUpdateTariff_withNegativeRates_thenPropagated() {
        TariffEntity t = new TariffEntity();
        when(tariffRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tariffRepository.save(t)).thenReturn(t);

        TariffEntity out = tariffService.updateTariff(-1_000.0, -500.0);
        assertThat(out.getDailyRentalRate()).isEqualTo(-1_000.0);
        assertThat(out.getDailyFineRate()).isEqualTo(-500.0);
    }

    @Test
    @DisplayName("updateTariff: Double.MIN_VALUE – overflow safe")
    void whenUpdateTariff_withMinValue_thenSaved() {
        TariffEntity t = new TariffEntity();
        when(tariffRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tariffRepository.save(t)).thenReturn(t);

        TariffEntity out = tariffService.updateTariff(Double.MIN_VALUE, Double.MIN_VALUE);
        assertThat(out.getDailyRentalRate()).isEqualTo(Double.MIN_VALUE);
    }

    @Test
    @DisplayName("updateTariff: NaN – se propaga (defensivo)")
    void whenUpdateTariff_withNaN_thenSaved() {
        TariffEntity t = new TariffEntity();
        when(tariffRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tariffRepository.save(t)).thenReturn(t);

        TariffEntity out = tariffService.updateTariff(Double.NaN, Double.POSITIVE_INFINITY);
        assertThat(out.getDailyRentalRate()).isNaN();
        assertThat(out.getDailyFineRate()).isInfinite();
    }

    @Test
    @DisplayName("updateTariff: múltiples updates consecutivos – último ganador")
    void whenRapidUpdates_thenLastWins() {
        TariffEntity shared = new TariffEntity();
        when(tariffRepository.findById(1L)).thenReturn(Optional.of(shared));
        when(tariffRepository.save(shared)).thenReturn(shared);

        tariffService.updateTariff(1.0, 1.0);
        tariffService.updateTariff(9.0, 9.0);
        tariffService.updateTariff(99.0, 99.0);

        assertThat(shared.getDailyRentalRate()).isEqualTo(99.0);
        assertThat(shared.getDailyFineRate()).isEqualTo(99.0);
        verify(tariffRepository, times(3)).save(shared);
    }

    @Test
    @DisplayName("updateTariff: proxy modifica entidad después de save – observable")
    void whenSaveIsProxy_thenMutationVisible() {
        TariffEntity t = new TariffEntity();
        when(tariffRepository.findById(1L)).thenReturn(Optional.of(t));
        // Simula un auditor que pone ID después de insert
        when(tariffRepository.save(t)).thenAnswer(i -> {
            TariffEntity x = i.getArgument(0);
            x.setId(1L);
            return x;
        });

        TariffEntity out = tariffService.updateTariff(3.0, 6.0);
        assertThat(out.getId()).isEqualTo(1L);
    }

    /* ---------- CONCURRENCY ---------- */

    @Test
    @DisplayName("updateTariff: 100 threads concurrentes – sin excepciones")
    void whenConcurrentUpdates_thenNoException() throws InterruptedException {
        TariffEntity shared = new TariffEntity();
        when(tariffRepository.findById(1L)).thenReturn(Optional.of(shared));
        when(tariffRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicReference<Exception> error = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            final int val = i;
            pool.submit(() -> {
                try {
                    tariffService.updateTariff(val * 1.0, val * 2.0);
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
    }


    /* ---------- GET ALL TARIFFS – COBERTURA FALTANTE ---------- */

    @Test
    @DisplayName("getAllTariffs: lista vacía → devuelve iterable vacío")
    void whenGetAllTariffs_empty_thenEmptyList() {
        when(tariffRepository.findAll()).thenReturn(List.of());

        List<TariffEntity> result = tariffService.getAllTariffs();

        assertThat(result).isEmpty();
        verify(tariffRepository).findAll();
    }

    @Test
    @DisplayName("getAllTariffs: lista con nulls → devuelve iterable con nulls")
    void whenGetAllTariffs_withNulls_thenReturnsIterableWithNulls() {
        when(tariffRepository.findAll()).thenReturn(Arrays.asList(null, null));

        List<TariffEntity> result = tariffService.getAllTariffs();

        assertThat(result).hasSize(2).containsOnlyNulls();
        verify(tariffRepository).findAll();
    }

    /* ---------- GET BY ID – COBERTURA FALTANTE ---------- */

    @Test
    @DisplayName("getTariffById: encontrado → devuelve entidad")
    void whenGetTariffById_found_thenReturns() {
        TariffEntity t = new TariffEntity();
        t.setId(2L);
        when(tariffRepository.findById(2L)).thenReturn(Optional.of(t));

        TariffEntity out = tariffService.getTariffById(2L);

        assertThat(out).isEqualTo(t);
        verify(tariffRepository).findById(2L);
    }

    @Test
    @DisplayName("getTariffById: no encontrado → lanza excepción")
    void whenGetTariffById_notFound_thenThrows() {
        when(tariffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tariffService.getTariffById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tariff not found");

        verify(tariffRepository).findById(99L);
    }

    /* ---------- UPDATE – COBERTURA FALTANTE ---------- */

    @Test
    @DisplayName("updateTariff: crear nueva cuando no existe id=1")
    void whenUpdateTariff_noId1_thenCreatesNew() {
        TariffEntity created = new TariffEntity();
        when(tariffRepository.findById(1L)).thenReturn(Optional.empty());
        when(tariffRepository.save(any(TariffEntity.class))).thenReturn(created);

        TariffEntity out = tariffService.updateTariff(50.0, 25.0);

        assertThat(out).isEqualTo(created);
        verify(tariffRepository).findById(1L);
        verify(tariffRepository).save(any(TariffEntity.class)); // ✅ cualquier instancia
    }

    /* ---------- CONCURRENCIA – COBERTURA FALTANTE ---------- */

    @Test
    @DisplayName("getAllTariffs: 100 hilos concurrentes – sin excepciones")
    void whenConcurrentGetAllTariffs_thenNoException() throws InterruptedException {
        when(tariffRepository.findAll()).thenReturn(List.of());

        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicReference<Exception> error = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    tariffService.getAllTariffs();
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
        verify(tariffRepository, times(threads)).findAll();
    }

    /* ---------- UTIL ---------- */

    @AfterEach
    void validateMocks() {
        validateMockitoUsage();
    }
}