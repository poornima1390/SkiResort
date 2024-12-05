import java.io.IOException;
import java.io.BufferedReader;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;



@WebServlet(value = "/skiers/*")

public class SkierServlet extends javax.servlet.http.HttpServlet {
    private Connection connection;
    private BlockingQueue<Channel> channelPool;
    private final static int POOL_SIZE = 20;
    private final static String QUEUE_NAME = "skiersQueue";

    private final SkierApiHandler skierApiHandler;
    private final VerticalApiHandler verticalApiHandler;
    private final Gson gson = new Gson();


    public SkierServlet() {
        this.skierApiHandler = new SkierApiHandler(DynamoDbClient.builder()
                .build());
        this.verticalApiHandler = new VerticalApiHandler(DynamoDbClient.builder()
                .build());
    }

    @Override
    public void init() throws ServletException {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("44.232.23.42");
            //factory.setHost("localhost");
            factory.setPort(5672);
            factory.setUsername("username");
            factory.setPassword("password");
            connection = factory.newConnection();


            channelPool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) {
                Channel channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                channelPool.offer(channel);
            }

        } catch (Exception e) {
            throw new ServletException("Error initializing RabbitMQ", e);
        }
    }

    @Override
    protected void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {

        response.setContentType("application/json");
        String urlPath = request.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");
        if (!isUrlValid(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"Invalid URL format\"}");
            return;
        }

        try {
            BufferedReader reader = request.getReader();
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            Integer skierID = jsonObject.get("skierID").getAsInt();
            Integer liftID = jsonObject.get("liftID").getAsInt();
            Integer resortID = jsonObject.get("resortID").getAsInt();
            Integer seasonID = jsonObject.get("seasonID").getAsInt();
            Integer dayID = jsonObject.get("dayID").getAsInt();
            Integer time = jsonObject.get("time").getAsInt();

            String validationResult = validateParameters(skierID, resortID, liftID, seasonID, dayID, time);
            if (!validationResult.equals("Valid")) {

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", validationResult);
                response.getWriter().write(errorResponse.toString());
                return;
            }

            Channel channel = channelPool.take();
            try {

                JsonObject message = new JsonObject();
                message.addProperty("skierID", skierID);
                message.addProperty("liftID", liftID);
                message.addProperty("resortID", resortID);
                message.addProperty("seasonID", seasonID);
                message.addProperty("dayID", dayID);
                message.addProperty("time", time);

                channel.basicPublish("", QUEUE_NAME, null, message.toString().getBytes());

                response.setStatus(HttpServletResponse.SC_CREATED);
                JsonObject successResponse = new JsonObject();
                successResponse.addProperty("message", "Ski data recorded successfully.");
                response.getWriter().write(successResponse.toString());

            } finally {

                channelPool.offer(channel);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Internal server error");
            response.getWriter().write(errorResponse.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        try {
            for (Channel channel : channelPool) {
                channel.close();
            }
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.destroy();
    }

    private String validateParameters(Integer skierID, Integer resortID, Integer liftID, Integer seasonID, Integer dayID, Integer time) {
        if (skierID == null || skierID < 1 || skierID > 100000) {
            return "Invalid skierID: must be between 1 and 100000.";
        }
        if (resortID == null || resortID < 1 || resortID > 10) {
            return "Invalid resortID: must be between 1 and 10.";
        }
        if (liftID == null || liftID < 1 || liftID > 40) {
            return "Invalid liftID: must be between 1 and 40.";
        }
        if (seasonID == null || !seasonID.equals(2024)) {
            return "Invalid seasonID: must be 2024.";
        }
        if (dayID == null || dayID < 1 || dayID > 3) {
            return "Invalid dayID: must be 1.";
        }
        if (time == null || time < 1 || time > 360) {
            return "Invalid time: must be between 1 and 360.";
        }
        return "Valid";
    }

    private boolean isUrlValid(String[] urlPath) {

        if (urlPath.length != 8) {
            return false;
        }

        try {

            int resortID = Integer.parseInt(urlPath[1]);
            if (resortID < 1 || resortID > 10)  return false;

            if (!urlPath[2].equals("seasons")) return false;

            int seasonID = Integer.parseInt(urlPath[3]);
            if (seasonID != 2024) return false;

            if (!urlPath[4].equals("days")) return false;

            int dayID = Integer.parseInt(urlPath[5]);
            if (dayID < 1 || dayID > 3) return false;

            if (!urlPath[6].equals("skiers")) return false;

            int skierID = Integer.parseInt(urlPath[7]);
            if (skierID < 1 || skierID > 100000) return false;

            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();
        System.out.println("Received URL Path: " + urlPath);
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");
        if (isGetSkierVertical(urlParts)) {
            verticalApiHandler.handleGetSkierVertical(urlParts, response);
        }
        else if (isUrlValid(urlParts)) {
            try {

                int resortID = Integer.parseInt(urlParts[1]);
                int seasonID = Integer.parseInt(urlParts[3]);
                int dayID = Integer.parseInt(urlParts[5]);
                int skierID = Integer.parseInt(urlParts[7]);

                Map<String, String> skierDetails = skierApiHandler.getSkierLiftRideDetails(resortID, seasonID, dayID, skierID);


                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson(skierDetails));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            }

        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Invalid URL format");
        }
    }
    private boolean isGetSkierVertical(String[] urlParts) {
        return urlParts.length == 3 && urlParts[2].equals("vertical");
    }




}








