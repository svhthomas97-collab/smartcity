/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcity2.smartcityprojectfinal;

/**
 *
 * @author trion
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

// This code is adapted from https://github.com/jmdns/jmdns
public class ServiceDiscovery {

    private String requiredServiceType;
    private String requiredServiceName;
    private JmDNS jmdns;
    // private String url; - removed from the original version to insert port
    private int Port = -1;

    public ServiceDiscovery(String inServiceType, String inServiceName) {
        requiredServiceType = inServiceType;
        requiredServiceName = inServiceName;
    }

    public int discoverService(long timeoutMilliseconds) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        try {

            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            System.out.println("Client: InetAddress.getLocalHost():" + InetAddress.getLocalHost());

            // Add a service listener that listens for the required service type on localhost
            jmdns.addServiceListener(requiredServiceType, new ServiceListener() {

                @Override
                public void serviceAdded(ServiceEvent event) {
                    ServiceInfo info = event.getInfo();
                    System.out.println("Service added: " + event.getInfo());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    System.out.println("Service removed: " + event.getInfo());
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    System.out.println("Service resolved: " + event.getInfo());
                    ServiceInfo info = event.getInfo();
                    int port = info.getPort();
                    String resolvedServiceName = info.getName();

                    System.out.println("####service " + resolvedServiceName + " resolved at: " + port);

                    /* if (resolvedServiceName.contains(requiredServiceName)) {
                        // the URL information that was discovered is passed onto the HttpClient
                        System.out.println("Discovered service named: " + resolvedServiceName);
                        String path = info.getNiceTextString().split("=")[1];

                        url = "http://localhost:" + port + "/" + path;
                        System.out.println(url);
                         // the event we were waiting for has happened. Release the latch.  
                         latch.countDown(); 
                    }REMOVED FROM THE LECTURER VERSION */

                    if (resolvedServiceName.equals(requiredServiceName)) {
                        System.out.println("Discovered service named: " + resolvedServiceName);
                        Port = port;
                        latch.countDown();
                    }

                }
            });

        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        // if there was no service resolved of the required type latch will timeoout
        latch.await(timeoutMilliseconds, TimeUnit.MILLISECONDS);
        System.out.println("Discover Service returning port: " + Port);
        return Port;
    }

    public void close() throws IOException {
        if (jmdns != null) {
            jmdns.close();
        }
    }

}
