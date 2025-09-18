package com.toolrent.services;

import com.toolrent.entities.TariffEntity;
import com.toolrent.repositories.TariffRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TariffService {

    private final TariffRepository tariffRepository;

    public TariffService(TariffRepository tariffRepository) {
        this.tariffRepository = tariffRepository;
    }

    public TariffEntity updateTariff(Double dailyRentalRate, Double dailyFineRate) {
        TariffEntity tariff = tariffRepository.findById(1L).orElse(new TariffEntity());
        tariff.setDailyRentalRate(dailyRentalRate);
        tariff.setDailyFineRate(dailyFineRate);
        return tariffRepository.save(tariff);
    }

    public List<TariffEntity> getAllTariffs() {          // devuelve TODAS
        return tariffRepository.findAll();
    }

    public TariffEntity getTariffById(Long id) {         // devuelve una por ID
        return tariffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tariff not found"));
    }

}