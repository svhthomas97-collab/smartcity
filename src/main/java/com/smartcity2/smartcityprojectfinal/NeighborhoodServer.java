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
public class NeighborhoodServer {

    private static final Logger logger = Logger.getLogger(NeighborhoodServer.class.getName());

    public static void main(String[] args) {
        int port = 50051;

        try {
            Server server = ServerBuilder.forPort(port)
                    .addService(new NeighborhoodServiceImpl())
                    .build();

            server.start();
            try {
                ServiceRegistration.getInstance().registerService(
                        "_grpc._tcp.local.",
                        "NeighborhoodService",
                        port,
                        "Neighborhood Service"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("NeighborhoodService started on port " + port);

            server.awaitTermination();

        } catch (IOException e) {
            logger.severe("Failed to start NeighborhoodService: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.severe("NeighborhoodService interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
