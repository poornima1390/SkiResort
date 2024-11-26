import java.io.IOException;
import java.io.BufferedReader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

@WebServlet(value = "/skiers/*")

public class SkierServlet extends javax.servlet.http.HttpServlet {
    private Connection connection;
    private BlockingQueue<Channel> channelPool;
    private final static int POOL_SIZE = 20;
    private final static String QUEUE_NAME = "skiersQueue";

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
        if (dayID == null || !dayID.equals(1)) {
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

            if (!urlPath[4].equals("day")) return false;

            int dayID = Integer.parseInt(urlPath[5]);
            if (dayID != 1) return false;

            if (!urlPath[6].equals("skier")) return false;

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

        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing paramterers");
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Invalid URL format");
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("It works!");
        }
    }

    }


