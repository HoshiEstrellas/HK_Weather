package com.example.hkweather.dao;

import com.example.hkweather.entity.SyncRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRunDao extends JpaRepository<SyncRunEntity, Long>, SyncRunDaoCustom {
}
