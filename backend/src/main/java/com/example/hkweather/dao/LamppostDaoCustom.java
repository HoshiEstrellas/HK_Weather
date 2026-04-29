package com.example.hkweather.dao;

import com.example.hkweather.dto.LamppostDto;
import com.example.hkweather.model.DeviceType;
import com.example.hkweather.model.LamppostLocation;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LamppostDaoCustom {

    void upsertAll(List<LamppostLocation> lampposts, Map<String, DeviceType> deviceTypes);

    List<LamppostDto> findAllViews();

    Optional<LamppostDto> findByLpNumberView(String lpNumber);
}
