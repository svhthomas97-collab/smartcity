/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcity2.smartcityprojectfinal;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.logging.Logger;
/**
 *
 * @author ThomasNCI
 */
public class AirQualityServer {

    private static final Logger logger = Logger.getLogger(AirQualityServer.class.getName());

    public static void main(String[] args) {
        int port = 50052;

        try {
            Server server = ServerBuilder.forPort(port)
                    .addService(new AirQualityServiceImpl())
                    .build();

            server.start();
            logger.info("AirQualityService started on port " + port);

            server.awaitTermination();

        } catch (IOException e) {
            logger.severe("Failed to start AirQualityService: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.severe("AirQualityServer interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}