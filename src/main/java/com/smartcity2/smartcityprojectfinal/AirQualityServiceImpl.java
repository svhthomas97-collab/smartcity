/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcity2.smartcityprojectfinal;

import generated.grpc.airquality.AirQuality;
import generated.grpc.airquality.AirQualityServiceGrpc.AirQualityServiceImplBase;
import generated.grpc.airquality.Sensor;
import generated.grpc.airquality.SensorResponse;
import generated.grpc.airquality.Zone;
import io.grpc.stub.StreamObserver;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 *
 * @author ThomasNCI
 *
 * This class handles all air quality related requests coming from the client
 * It extends the auto-generated gRPC base class so we can override its methods
 */
public class AirQualityServiceImpl extends AirQualityServiceImplBase {

    private static final Logger logger = Logger.getLogger(AirQualityServiceImpl.class.getName());

    // Map that stores all registered sensors
    // The key is the sensor ID, the value is its location
    private static final Map<String, String> registeredSensors = new HashMap<>();

    // Random object used to generate simulated sensor readings
    private static final Random random = new Random();

    // 1. UNARY - client sends 1 request with sensor data, server responds with 1 response
    @Override
    public void registerSensor(Sensor request, StreamObserver<SensorResponse> responseObserver) {

        // Get the sensor ID and location sent by the client
        String sensorId = request.getSensorId();
        String location = request.getLocation();

        // If the sensor ID is empty, send an error
        if (sensorId == null || sensorId.trim().isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("Sensor ID can't be empty."));
            return;
        }

        // If the location is empty, send an error
        if (location == null || location.trim().isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("Sensor location can't be empty."));
            return;
        }

        // Store the sensor on Map and print a confirmation to the console
        registeredSensors.put(sensorId, location);
        System.out.println(LocalTime.now().toString() + ": sensor registered - " + sensorId + " at " + location);

        // Build the response object confirming the registration was successful
        SensorResponse response = SensorResponse.newBuilder()
                .setStatus("Sensor " + sensorId + " registered successfully at " + location)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // 2. SERVER STREAMING - client sends 1 request, server responds with multiple responses
    @Override
    public void monitorAirQuality(Zone request, StreamObserver<AirQuality> responseObserver) {

        // Get the zone name that the client wants to monitor
        String zoneName = request.getZone();

        // If the zone name is empty, send an error
        if (zoneName == null || zoneName.trim().isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("Zone cannot be empty."));
            return;
        }

        try {
            // Simulate 5 sensor readings for the zone and stream them one by one to the client
            for (int i = 0; i < 5; i++) {

                // Generate random values for each pollutant
                double pm25 = 10 + (random.nextDouble() * 90);  // fine particles (µg/m³)
                double co2  = 300 + (random.nextDouble() * 900); // carbon dioxide (ppm)
                double no2  = 20 + (random.nextDouble() * 180);  // nitrogen dioxide (µg/m³)

                // Classify air quality
                String level;
                if (pm25 > 70 || co2 > 1000 || no2 > 150) {
                    level = "Dangerous"; // at least one pollutant has reached a dangerous level
                } else if (pm25 > 35 || co2 > 700 || no2 > 80) {
                    level = "Moderate";  // at least one pollutant is at a moderate level
                } else {
                    level = "Good";      // all pollutants are within safe limits
                }

                System.out.println(LocalTime.now().toString() + ": sending reading for zone " + zoneName + " -> " + level);

                // Get all the data and creates response
                AirQuality response = AirQuality.newBuilder()
                        .setZone(zoneName)
                        .setPm25(pm25)
                        .setCo2(co2)
                        .setNo2(no2)
                        .setLevel(level)
                        .build();

                responseObserver.onNext(response);

                Thread.sleep(1000);
            }

            responseObserver.onCompleted();

        } catch (InterruptedException e) {
            logger.severe("Stream interrupted while monitoring zone: " + zoneName + " - " + e.getMessage());
            responseObserver.onError(e);
            Thread.currentThread().interrupt();
        }
    }
}