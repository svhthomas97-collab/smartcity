/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcity2.smartcityprojectfinal;

import generated.grpc.budget.BudgetAllocationServiceGrpc.BudgetAllocationServiceImplBase;
import generated.grpc.budget.BudgetPlan;
import generated.grpc.budget.BudgetPriorityRequest;
import generated.grpc.budget.NeighborhoodBudgetRequest;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 *
 * @author ThomasNCI
 *
 * This class handles all BudgetAllocation related requests coming from the client It
 * extends the auto-generated gRPC base class so we can override its methods
 */
public class BudgetAllocationServiceImpl extends BudgetAllocationServiceImplBase {

    private static final Logger logger = Logger.getLogger(BudgetAllocationServiceImpl.class.getName());

    // Map that stores the start budget of each neighborhood
    // The key is the sensor ID, the value is its location
    private static final Map<String, Double> startBudgets = new HashMap<>();

    static {
        // "ID", how much money the Neighborhood has at the start
        startBudgets.put("N001", 1000.0);
        startBudgets.put("N002", 2000.0);
        startBudgets.put("N003", 3000.0);
        startBudgets.put("N004", 3500.0);
        startBudgets.put("N005", 4200.0);
    }
    // 1. UNARY - client sends 1 request with sensor data, server responds with 1 response

    @Override
    public void getBudgetPlan(NeighborhoodBudgetRequest request,
            StreamObserver<BudgetPlan> responseObserver) {
        //gets the Id sent by the client
        String id = request.getNeighborhoodId();
        //gets the start budget
        Double start = startBudgets.get(id);
        // if doesnt find the neighborhood = error
        if (start == null) {
            responseObserver.onError(new IllegalArgumentException("Neighborhood not found: " + id));
            return;
        }
        //set response values
        BudgetPlan response = BudgetPlan.newBuilder()
                .setNeighborhoodId(id)
                .setAllocatedAmount(start)
                .setStatus("allocation")
                .build();
        //sends response 
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // 2. BiDi Streaming - client and server communicates in real time
    @Override
    public StreamObserver<BudgetPriorityRequest> allocateBudget(
            StreamObserver<BudgetPlan> responseObserver) {

        return new StreamObserver<BudgetPriorityRequest>() {

            
            @Override
            public void onNext(BudgetPriorityRequest request) {
                //data received
                String id = request.getNeighborhoodId();
                int priority = request.getPriorityLevel();
                //if start budget doesnt exist, use 5000 as standard.
                Double start = startBudgets.getOrDefault(id, 5000.0);

                // adjust budget based on priority, more priority = more money allocated
                double allocated = start + (priority * 1000);
                //priority score definition
                String status;
                if (priority >= 8) {
                    status = "High Priority - Emergency Allocation";
                } else if (priority >= 5) {
                    status = "Medium Priority";
                } else {
                    status = "Low Priority";
                }

                BudgetPlan response = BudgetPlan.newBuilder()
                        .setNeighborhoodId(id)
                        .setAllocatedAmount(allocated)
                        .setStatus(status)
                        .build();

                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error in allocateBudget: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
