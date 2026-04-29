package com.example.hkweather.dao;

import com.example.hkweather.entity.LamppostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LamppostDao extends JpaRepository<LamppostEntity, String>, LamppostDaoCustom {
}
