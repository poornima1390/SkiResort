package com.example.client2;

import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.example.SkiDataGenerator;
import com.example.SkiersHttpClient;
import com.opencsv.CSVWriter;

public class MultiThreadedClient2{
    final static private int NUMTHREADS = 180;
    final static private int NUM_REQUESTS=2000;
    private static int MAX_RETRIES = 5;
    static int countYay=0;
    static int countNay=0;

    static List<Long> responseTimes=new ArrayList<>();
    static long latency;
    static long responseTime=0;
    private static String URI="http://localhost:8080/JAVAservlets_war_exploded/skiers/9/seasons/2024/day/1/skier/123";
    //private static String URI = "http://44.225.18.127:8080/JAVAservlets_war/skiers/9/seasons/2024/day/1/skiers/123";//JAVA Servlet
    //private static String URI="http://35.163.98.107:8085/skiers";//Spring Server
    //private static String URI="http://myservers-960862116.us-west-2.elb.amazonaws.com:8080/JAVAservlets_war/skiers/9/seasons/2024/day/1/skier/123";

    synchronized public static void  increment(){
        countYay++;
    }
    synchronized public static void incrementFailed(){countNay++;}


    public static void main(String[] args) throws InterruptedException, IOException {
        ExecutorService warmUpExecutor = Executors.newFixedThreadPool(32);
        CountDownLatch latch=new CountDownLatch(1);

        for (int i = 1; i <= 32; i++) {
            warmUpExecutor.submit(() -> {
                for (int j = 1; j <= 1000; j++) {
                    String skiEvent1 = SkiDataGenerator.generateSkiEvent();
                    try {
                        HttpResponse<String> response1 = SkiersHttpClient.sendPostRequest(skiEvent1, URI);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                latch.countDown();
            });
        }
        latch.await();

        CSVWriter writer = new CSVWriter(new FileWriter("response.csv"));
        String[] header = {"StartTime", "Request Type","Latency","Response code"};
        writer.writeNext(header);

        ExecutorService executor= Executors.newFixedThreadPool(NUMTHREADS);
        long startTime=System.currentTimeMillis();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            executor.execute(() -> {

                try {
                    String skiEvent = SkiDataGenerator.generateSkiEvent();
                    long startTime2=System.currentTimeMillis();
                    HttpResponse<String> response = SkiersHttpClient.sendPostRequest(skiEvent, URI);
                    long endTime2=System.currentTimeMillis();

                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        increment();

                    } else {
                        for(int j=0;j<=MAX_RETRIES;j++){
                            response = SkiersHttpClient.sendPostRequest(skiEvent, URI);
                            if (response.statusCode() == 201){
                                increment();
                                break;
                            }
                        }
                        System.out.println("Request failed with status code: " + response.statusCode());
                        incrementFailed();
                    }
                    latency=endTime2-startTime2;
                    String[] row = {String.valueOf(startTime2), "POST", String.valueOf(latency), String.valueOf(response.statusCode())};
                    writer.writeNext(row);
                    responseTime+=latency;
                    responseTimes.add(latency);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        warmUpExecutor.shutdown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        long endTime=System.currentTimeMillis();
        Collections.sort(responseTimes);

        System.out.println("Successfully sent");
        System.out.println("Number of Threads: "+NUMTHREADS);
        System.out.println("Number of successful requests: "+countYay);
        System.out.println("Number of unsuccessful request: "+countNay);
        double threadExecS=(endTime-startTime)/1000;
        System.out.println("Thread execution time: " + (endTime-startTime) + " ms");
        System.out.println("Throughput: "+(countYay/threadExecS)+" requests per second");
        System.out.println("Mean Response Time: "+(responseTime/countYay)+" ms");

        int middleIndex = responseTimes.size() / 2;
        double medianResponseTime;
        if (responseTimes.size() % 2 == 1) {
            medianResponseTime = responseTimes.get(middleIndex);
        } else {
            medianResponseTime = (responseTimes.get(middleIndex - 1) + responseTimes.get(middleIndex)) / 2.0;
        }
        System.out.println("Median Response Time: "+medianResponseTime+" ms");

        int p99Index = (int) Math.ceil(0.99 * responseTimes.size());
        long p99ResponseTime = responseTimes.get(p99Index - 1);
        System.out.println("p99 Response Time: "+p99ResponseTime+" ms");

        System.out.println("Min Response Time: "+Collections.min(responseTimes)+" ms");
        System.out.println("Max Response Time: "+Collections.max(responseTimes)+" ms");
    }

}

