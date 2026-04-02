/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcity2.smartcityprojectfinal;

import io.grpc.Status;
import smartcity.generated.neighborhood.NeighborhoodList;
import smartcity.generated.neighborhood.NeighborhoodRequest;
import smartcity.generated.neighborhood.NeighborhoodServiceGrpc.NeighborhoodServiceImplBase;
import smartcity.generated.neighborhood.NeighborhoodStatus;

import io.grpc.stub.StreamObserver;
import java.time.LocalTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author ThomasNCI
 *
 * This class handles all neighborhood related requests coming from the client
 * It extends the auto-generated gRPC base class so we can override its methods
 */
public class NeighborhoodServiceImpl extends NeighborhoodServiceImplBase {

    // Logger is used to print messages to the console, useful for debugging
    private static final Logger logger = Logger.getLogger(NeighborhoodServiceImpl.class.getName());

    // Map that stores neighborhood codes and values
    // The key is the neighborhood ID (i.e: N001), the value is an array with 3 numbers
    private static final Map<String, double[]> neighborhoodData = new HashMap<>();

    static {
        // "ID" {infrastructure, resources, accessibility} - higher number = better neighborhood condition
        neighborhoodData.put("N001", new double[]{30, 40, 35});
        neighborhoodData.put("N002", new double[]{23, 12, 10});
        neighborhoodData.put("N003", new double[]{23, 54, 12});
        neighborhoodData.put("N004", new double[]{34, 12, 14});
        neighborhoodData.put("N005", new double[]{90, 23, 12});
        neighborhoodData.put("N006", new double[]{45, 50, 48});
        neighborhoodData.put("N007", new double[]{20, 25, 30});
        neighborhoodData.put("N008", new double[]{85, 80, 88});
        neighborhoodData.put("N009", new double[]{38, 42, 40});
        neighborhoodData.put("N010", new double[]{72, 68, 74});
    }

    // 1. UNARY - client sends 1 request, server responds with 1 response
    @Override
    public void getNeighborhoodStatus(NeighborhoodRequest request,
            StreamObserver<NeighborhoodStatus> responseObserver) {

        // Get the neighborhood ID sent by the client (i.e: N001)
        String id = request.getNeighborhoodId();

        // Look up this ID in the Map to get the 3 scores
        double[] values = neighborhoodData.get(id);

        // If the ID doesn't exist in the Map, send an error to the client and stop
        if (values == null) {
            responseObserver.onError(new IllegalArgumentException("Neighborhood not found: " + id));
            return;
        }

        // Calculate the average of the 3 scores to get a general score
        // I.E: (30 + 40 + 35) / 3 = 35.0
        double score = (values[0] + values[1] + values[2]) / 3.0;
        System.out.println(LocalTime.now().toString() + ": status requested for " + id + " -> score: " + score);

        // Build the response object with all the neighborhood information
        NeighborhoodStatus response = NeighborhoodStatus.newBuilder()
                .setNeighborhoodId(id)
                .setScore(score)
                .setClassification(classify(score)) // "Critical", "AtRisk" or "Stable"
                .setInformalSettlement(score < 45) // true if the score is less than 45
                .setDescription(descriptionFor(classify(score)))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // 2. CLIENT STREAMING - client sends many requests, server responds with 1 response
    @Override
    public StreamObserver<NeighborhoodRequest> analyzeNeighborhoods(
            StreamObserver<NeighborhoodList> responseObserver) {

        return new StreamObserver<NeighborhoodRequest>() {

            // Gathers each status as requests come in
            List<NeighborhoodStatus> collected = new ArrayList<>();

            @Override
            // When a new neighborhood arrives, calculate its score and add to the list
            public void onNext(NeighborhoodRequest request) {
                String id = request.getNeighborhoodId();
                double[] values = neighborhoodData.get(id);

                if (values == null) {
                    responseObserver.onError(
                            Status.NOT_FOUND
                                    .withDescription("Neighborhood not found: " + id)
                                    .asRuntimeException()
                    );
                    return;
                }

                // Calculate the average score and add the neighborhood to the list
                double score = (values[0] + values[1] + values[2]) / 3.0;
                System.out.println(LocalTime.now().toString() + ": received a NeighborhoodRequest: " + id);

                collected.add(NeighborhoodStatus.newBuilder()
                        .setNeighborhoodId(id)
                        .setScore(score)
                        .setClassification(classify(score))
                        .setInformalSettlement(score < 45)
                        .setDescription(descriptionFor(classify(score)))
                        .build());
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Error during neighborhood stream: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.printf(LocalTime.now().toString() + ": NeighborhoodRequest stream complete \n");
                // Sort the list to show in Critical status order
                // Lowest score = worst condition = more urgency
                collected.sort((a, b) -> Double.compare(a.getScore(), b.getScore()));

                responseObserver.onNext(NeighborhoodList.newBuilder()
                        .addAllNeighborhoods(collected)
                        .build());

                responseObserver.onCompleted();
            }
        };
    }

    // Classify if the neighborhood is Critical, AtRisk or Stable using the score
    private String classify(double score) {
        if (score < 40) {
            return "Critical";
        }
        if (score < 70) {
            return "AtRisk";
        }
        return "Stable";
    }

    private String descriptionFor(String classification) {
        switch (classification) {
            case "Critical":
                return "Neighborhood conditions are: Critical - Needs immediate attention";
            case "AtRisk":
                return "Neighborhood conditions are: At Risk - Needs attention.";
            default:
                return "Neighborhood conditions are: Stable.";
        }
    }
}
