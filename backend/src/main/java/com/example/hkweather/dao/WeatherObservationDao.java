package com.example.hkweather.dao;

import com.example.hkweather.entity.WeatherObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherObservationDao extends JpaRepository<WeatherObservationEntity, Long>, WeatherObservationDaoCustom {
}
