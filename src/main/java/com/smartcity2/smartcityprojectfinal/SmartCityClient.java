/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcity2.smartcityprojectfinal;

import generated.grpc.airquality.AirQuality;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import smartcity.generated.neighborhood.NeighborhoodList;
import smartcity.generated.neighborhood.NeighborhoodRequest;
import smartcity.generated.neighborhood.NeighborhoodServiceGrpc;
import smartcity.generated.neighborhood.NeighborhoodStatus;

import generated.grpc.airquality.AirQualityServiceGrpc;
import generated.grpc.airquality.Sensor;
import generated.grpc.airquality.SensorResponse;
import generated.grpc.airquality.Zone;

/**
 *
 * @author ThomasNCI
 * This class is the gRPC client (before GUI is implemented, just to test)
 */
public class SmartCityClient {

    //private final ManagedChannel channel; declaration used only with NeighborhoodServer
    private final ManagedChannel neighborhoodChannel;
    private final ManagedChannel airQualityChannel;
    //NeighborhoodService Stub (UNARY, STREAMING)
    private final NeighborhoodServiceGrpc.NeighborhoodServiceBlockingStub neighborhoodBlockingStub;
    private final NeighborhoodServiceGrpc.NeighborhoodServiceStub neighborhoodAsyncStub;
    // AirQuality Stub (UNARY, STREAMING)
    private final AirQualityServiceGrpc.AirQualityServiceBlockingStub airBlockingStub;
    private final AirQualityServiceGrpc.AirQualityServiceStub airAsyncStub;

    public SmartCityClient() {
        /*        channel = ManagedChannelBuilder.forAddress("localhost", 50051)
        .usePlaintext()
        .build();*/

        neighborhoodChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        airQualityChannel = ManagedChannelBuilder.forAddress("localhost", 50052)
                .usePlaintext()
                .build();

        neighborhoodBlockingStub = NeighborhoodServiceGrpc.newBlockingStub(neighborhoodChannel);
        neighborhoodAsyncStub = NeighborhoodServiceGrpc.newStub(neighborhoodChannel);
        airBlockingStub = AirQualityServiceGrpc.newBlockingStub(airQualityChannel);
        airAsyncStub = AirQualityServiceGrpc.newStub(airQualityChannel);
    }

    //NeighborhooStatus - UNARY 
    public void getNeighborhoodStatus(String neighborhoodId) {
        //Creates request with Neighborhood ID
        NeighborhoodRequest request = NeighborhoodRequest.newBuilder()
                .setNeighborhoodId(neighborhoodId)
                .build();

        NeighborhoodStatus response = neighborhoodBlockingStub.getNeighborhoodStatus(request);
        //prints the information
        System.out.println("Neighborhood ID: " + response.getNeighborhoodId());
        System.out.println("Score: " + response.getScore());
        System.out.println("Classification: " + response.getClassification());
        System.out.println("Informal Settlement: " + response.getInformalSettlement());
        System.out.println("Description: " + response.getDescription());
    }

    //AirQuality - UNARY
    public void registerSensor(String sensorId, String location) {
        //Creates request with ID and sensor location
        Sensor request = Sensor.newBuilder()
                .setSensorId(sensorId)
                .setLocation(location)
                .build();

        SensorResponse response = airBlockingStub.registerSensor(request);
        // prints the information
        System.out.println("Sensor registration response: " + response.getStatus());
    }

    //NeighborhooStatus - CLIENT STREAMING
    public void analyzeNeighborhoods(List<String> neighborhoodIds) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<NeighborhoodList> responseObserver = new StreamObserver<NeighborhoodList>() {
            @Override
            public void onNext(NeighborhoodList response) {
                System.out.println("Neighborhoods sorted by urgency:");
                for (NeighborhoodStatus status : response.getNeighborhoodsList()) {
                    System.out.println(status.getNeighborhoodId()
                            + " | Score: " + status.getScore()
                            + " | Classification: " + status.getClassification()
                            + " | Informal: " + status.getInformalSettlement()
                            + " | Description: " + status.getDescription());
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error in analyzeNeighborhoods: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<NeighborhoodRequest> requestObserver
                = neighborhoodAsyncStub.analyzeNeighborhoods(responseObserver);

        for (String id : neighborhoodIds) {
            NeighborhoodRequest request = NeighborhoodRequest.newBuilder()
                    .setNeighborhoodId(id)
                    .build();
            requestObserver.onNext(request);
        }

        requestObserver.onCompleted();
        latch.await(5, TimeUnit.SECONDS);
    }

    //AirQuality
    public void monitorAirQuality(String zone) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<AirQuality> responseObserver = new StreamObserver<AirQuality>() {

            @Override
            public void onNext(AirQuality response) {
                System.out.println("Zone: " + response.getZone()
                        + " | PM2.5: " + response.getPm25()
                        + " | CO2: " + response.getCo2()
                        + " | NO2: " + response.getNo2()
                        + " | Level: " + response.getLevel());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error in monitorAirQuality: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Air quality stream completed.");
                System.out.println("-----------------------------------");
                latch.countDown();
            }
        };

        Zone request = Zone.newBuilder()
                .setZone(zone)
                .build();

        airAsyncStub.monitorAirQuality(request, responseObserver);

        latch.await(10, TimeUnit.SECONDS);
    }

    public void shutdown() throws InterruptedException {

        //channel.shutdown().awaitTermination(5, TimeUnit.SECONDS); line used when testing only Neighborhood, now with AirQ it needs two channels.
        neighborhoodChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        airQualityChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        SmartCityClient client = new SmartCityClient();

        try {
            // Neighborhood tests
            client.getNeighborhoodStatus("N001");
            client.analyzeNeighborhoods(Arrays.asList("N001", "N003", "N005"));

            // Air Quality tests
            client.registerSensor("S001", "Downtown");
            client.registerSensor("S002", "Industrial Zone");

            client.monitorAirQuality("Downtown");

        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } finally {
            try {
                client.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }
}