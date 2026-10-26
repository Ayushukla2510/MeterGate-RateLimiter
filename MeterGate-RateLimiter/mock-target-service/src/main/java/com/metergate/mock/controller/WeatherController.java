package com.metergate.mock.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {

    /**
     * Protected dummy endpoint that clients want to access.
     * Example: GET /api/v1/weather?city=Mumbai
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWeather(
            @RequestParam(name = "city", defaultValue = "Delhi") String city) {

        // Returning dummy weather data as a JSON map
        Map<String, Object> weatherData = Map.of(
                "city", city,
                "temperatureCelsius", 28.5,
                "condition", "Partly Cloudy",
                "humidityPercentage", 65,
                "windSpeedKmh", 14.2,
                "timestamp", LocalDateTime.now().toString(),
                "source", "MeterGate Mock Weather Provider"
        );

        return ResponseEntity.ok(weatherData);
    }
}