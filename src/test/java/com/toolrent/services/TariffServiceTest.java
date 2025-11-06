package com.toolrent.services;

import com.toolrent.entities.TariffEntity;
import com.toolrent.repositories.TariffRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock private TariffRepository tariffRepository;

    @InjectMocks private TariffService tariffService;

    /* ======================================================================
              1. updateTariff – (update / create)
       ====================================================================== */

    @Test @DisplayName("updateTariff – tarifa existe → actualiza")
    void updateTariff_exists(){
        TariffEntity current = new TariffEntity();
        current.setId(1L);
        current.setDailyRentalRate(1000.0);
        current.setDailyFineRate(500.0);

        when(tariffRepository.findById(1L)).thenReturn(Optional.of(current));
        when(tariffRepository.save(any(TariffEntity.class))).thenAnswer(i -> i.getArgument(0));

        TariffEntity res = tariffService.updateTariff(1500.0, 750.0);

        assertThat(res.getDailyRentalRate()).isEqualTo(1500.0);
        assertThat(res.getDailyFineRate()).isEqualTo(750.0);
        verify(tariffRepository).save(current);
    }

    @Test @DisplayName("updateTariff – no existe → crea nueva con ID 1")
    void updateTariff_notExists(){
        when(tariffRepository.findById(1L)).thenReturn(Optional.empty());

        when(tariffRepository.save(any(TariffEntity.class))).thenAnswer(i -> {
            TariffEntity t = i.getArgument(0);
            t.setId(1L); // simulamos que la BD asigna ID
            return t;
        });

        TariffEntity res = tariffService.updateTariff(1200.0, 600.0);

        assertThat(res.getDailyRentalRate()).isEqualTo(1200.0);
        assertThat(res.getDailyFineRate()).isEqualTo(600.0);
        verify(tariffRepository).save(res);
    }


    /* ======================================================================
              2. getAllTariffs – (vacía / con datos)
       ====================================================================== */

    @Test @DisplayName("getAllTariffs – con datos")
    void getAllTariffs_withData(){
        List<TariffEntity> list = List.of(
                mock(TariffEntity.class),
                mock(TariffEntity.class));
        when(tariffRepository.findAll()).thenReturn(list);

        List<TariffEntity> res = tariffService.getAllTariffs();

        assertThat(res).hasSize(2);
    }

    @Test @DisplayName("getAllTariffs – vacía")
    void getAllTariffs_empty(){
        when(tariffRepository.findAll()).thenReturn(List.of());

        List<TariffEntity> res = tariffService.getAllTariffs();

        assertThat(res).isEmpty();
    }

    /* ======================================================================
              3. getTariffById – (existe / no existe)
       ====================================================================== */

    @Test @DisplayName("getTariffById – existe")
    void getTariffById_exists(){
        TariffEntity tariff = new TariffEntity();
        tariff.setId(5L);
        when(tariffRepository.findById(5L)).thenReturn(Optional.of(tariff));

        TariffEntity res = tariffService.getTariffById(5L);

        assertThat(res).isEqualTo(tariff);
    }

    @Test @DisplayName("getTariffById – no existe → excepción")
    void getTariffById_notFound(){
        when(tariffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tariffService.getTariffById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tariff not found");
    }
}