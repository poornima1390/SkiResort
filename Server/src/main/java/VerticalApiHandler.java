import com.google.gson.JsonObject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
public class VerticalApiHandler {
    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "LiftRides";
    private final static int VERTICAL_FACTOR = 10;

    public VerticalApiHandler(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = DynamoDbClient.builder()
                .build();
    }
    public void handleGetSkierVertical(String[] urlParts, HttpServletResponse response) throws IOException {
        try {
            int skierID = Integer.parseInt(urlParts[1]);

            // Validate skierID
            if (skierID < 1 || skierID > 100000) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Invalid skierID: must be between 1 and 100000.");
                return;
            }

            // Fetch total vertical for skierID
            int totalVertical = fetchTotalVertical(skierID);

            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("skierID", skierID);
            jsonResponse.addProperty("totalVertical", totalVertical);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonResponse.toString());
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid skierID format");
        }
    }

    private int fetchTotalVertical(int skierID) {
        int totalVertical = 0;

        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("SkierID = :skierID")
                    .expressionAttributeValues(Map.of(
                            ":skierID", AttributeValue.builder().n(String.valueOf(skierID)).build()
                    ))
                    .build();

            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

            if (queryResponse.items().isEmpty()) {
                return 0;
            }

            for (Map<String, AttributeValue> item : queryResponse.items()) {
                int liftID = Integer.parseInt(item.get("LiftID").n());
                totalVertical += liftID * VERTICAL_FACTOR;
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch skier vertical: " + e.getMessage());
            e.printStackTrace();
        }

        return totalVertical;
    }
}
