package com.example.hkweather;

import com.example.hkweather.config.WeatherProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(WeatherProperties.class)
public class HkWeatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(HkWeatherApplication.class, args);
    }
}
