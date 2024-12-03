package com.example.client1;

import com.example.SkiDataGenerator;
import com.example.SkiersHttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultiThreadedClient{
    final static private int NUMTHREADS = 180;
    final static private int NUM_REQUESTS=200000;
    private static int MAX_RETRIES = 5;
    static int countYay=0;
    static int countNay=0;

    private static String URI = "http://35.163.98.107:8080/JAVAservlets_war/skiers/12/seasons/2019/day/1/skier/123";//JAVA Servlet
    //private static String URI="http://35.163.98.107:8085/skiers";//Spring Server

    synchronized public static void  increment(){countYay++;}
    synchronized public static void incrementFailed(){countNay++;}

    public void startUp() throws InterruptedException {
        ExecutorService warmUpExecutor = Executors.newFixedThreadPool(32);
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
            });
        }
        warmUpExecutor.shutdown();
        warmUpExecutor.awaitTermination(1, TimeUnit.MINUTES);
    }

    public static void main(String[] args)throws InterruptedException{

        MultiThreadedClient obj=new MultiThreadedClient();
        obj.startUp();

        ExecutorService executor= Executors.newFixedThreadPool(NUMTHREADS);
        long startTime=System.currentTimeMillis();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            executor.execute(() -> {

                    try {
                        String skiEvent = SkiDataGenerator.generateSkiEvent();
                        HttpResponse<String> response = SkiersHttpClient.sendPostRequest(skiEvent, URI);

                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            increment();

                        } else {
                            for(int j=0;j<=MAX_RETRIES;j++){
                                response = SkiersHttpClient.sendPostRequest(skiEvent, URI);
                                if (response.statusCode() >= 200 && response.statusCode() < 300){
                                    increment();
                                    break;
                                }
                            }
                            System.out.println("Request failed with status code: " + response.statusCode());
                            incrementFailed();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        long endTime=System.currentTimeMillis();

        System.out.println("Successfully sent");
        System.out.println("Number of Threads: "+NUMTHREADS);
        System.out.println("Number of successful requests: "+countYay);
        System.out.println("Number of unsuccessful request: "+countNay);
        System.out.println("Thread execution time: " + (endTime-startTime) + " ms");
        System.out.println("Throughput: "+(countYay/((endTime-startTime)/1000))+" requests per second");
    }

}
