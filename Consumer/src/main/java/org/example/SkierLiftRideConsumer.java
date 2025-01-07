package org.example;

import com.rabbitmq.client.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;



public class SkierLiftRideConsumer {

    private static final String QUEUE_NAME = "skiersQueue";
    //private static final String HOST = "localhost";
    private static final String HOST = "44.232.23.42";
    private static final int PORT = 5672;
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final String DYNAMODB_TABLE_NAME = "Lift_Rides";
    private static DynamoDbClient dynamoDbClient;

    public static void main(String[] args) {
        dynamoDbClient = DynamoDbClient.builder()
                .build();


        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);

        Connection connection = null;
        Channel channel = null;

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            ExecutorService executorService = Executors.newFixedThreadPool(20);

            channel.basicConsume(QUEUE_NAME, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    executorService.submit(() -> processMessage(body));
                }
            });


        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        } finally {

            Channel finalChannel = channel;
            Connection finalConnection = connection;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (finalChannel != null) {
                        finalChannel.close();
                    }
                    if (finalConnection != null) {
                        finalConnection.close();
                    }
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }));
        }

    }
    private static void processMessage(byte[] body) {
        try {

        String messageBody = new String(body, StandardCharsets.UTF_8);
        JsonObject jsonMessage = JsonParser.parseString(messageBody).getAsJsonObject();

        int skierID = jsonMessage.get("skierID").getAsInt();
        int liftID = jsonMessage.get("liftID").getAsInt();
        int resortID = jsonMessage.get("resortID").getAsInt();
        int seasonID = jsonMessage.get("seasonID").getAsInt();
        int dayID = jsonMessage.get("dayID").getAsInt();
        int time = jsonMessage.get("time").getAsInt();


        Map<String, AttributeValue> item = new HashMap<>();
        item.put("SkierID", AttributeValue.builder().n(String.valueOf(skierID)).build());
        item.put("LiftID", AttributeValue.builder().n(String.valueOf(liftID)).build());
        item.put("ResortID", AttributeValue.builder().n(String.valueOf(resortID)).build());
        item.put("SeasonID", AttributeValue.builder().n(String.valueOf(seasonID)).build());
        item.put("DayID", AttributeValue.builder().n(String.valueOf(dayID)).build());
        item.put("Time", AttributeValue.builder().n(String.valueOf(time)).build());
        item.put("EntryDetail",AttributeValue.builder().s(seasonID+"-"+dayID+"-"+time).build());


        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(DYNAMODB_TABLE_NAME)
                .item(item)
                .build();

                dynamoDbClient.putItem(putItemRequest);

        } catch (Exception e) {
            System.err.println("Failed to insert: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
