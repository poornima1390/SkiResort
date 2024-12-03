import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@WebServlet(value = "/resorts/*")
public class ResortServlet extends HttpServlet {

  private DynamoDbClient dynamoDbClient;
  private static final String DYNAMODB_TABLE_NAME = "LiftRides";

  @Override
  public void init() throws ServletException {
    try {
      // Initialize the DynamoDB client
      dynamoDbClient = DynamoDbClient.builder().build();
    } catch (Exception e) {
      throw new ServletException("Error initializing DynamoDB client", e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("application/json");
    String urlPath = request.getPathInfo();

    // Check if the URL path is valid
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("{\"message\": \"Missing parameters\"}");
      return;
    }

    String[] urlParts = urlPath.split("/");

    // Expected URL format: /{resortID}/seasons/{seasonID}/day/{dayID}/skiers
    if (!isUrlValid(urlParts)) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("{\"message\": \"Invalid URL format\"}");
      return;
    }

    int resortID = Integer.parseInt(urlParts[1]);
    int seasonID = Integer.parseInt(urlParts[3]);
    int dayID = Integer.parseInt(urlParts[5]);

    try {
      // Query DynamoDB to get the number of unique skiers
      int numSkiers = getUniqueSkiers(resortID, seasonID, dayID);

      response.setStatus(HttpServletResponse.SC_OK);

      JsonObject jsonResponse = new JsonObject();
      jsonResponse.addProperty(
          "time",
          "Resort" + resortID); // Replace with actual resort name if available
      jsonResponse.addProperty("numSkiers", numSkiers);

      response.getWriter().write(jsonResponse.toString());

    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      JsonObject errorResponse = new JsonObject();
      errorResponse.addProperty("message", "Internal server error");
      response.getWriter().write(errorResponse.toString());
      e.printStackTrace();
    }
  }

  private boolean isUrlValid(String[] urlParts) {
    // Expected format: /{resortID}/seasons/{seasonID}/day/{dayID}/skiers
    if (urlParts.length != 7) {
      return false;
    }

    try {
      int resortID = Integer.parseInt(urlParts[1]);
      if (resortID < 1 || resortID > 10)
        return false;

      if (!urlParts[2].equals("seasons"))
        return false;

      int seasonID = Integer.parseInt(urlParts[3]);
      if (seasonID != 2024)
        return false;

      if (!urlParts[4].equals("day"))
        return false;

      int dayID = Integer.parseInt(urlParts[5]);
      if (dayID != 1)
        return false;

      if (!urlParts[6].equals("skiers"))
        return false;

      return true;

    } catch (NumberFormatException e) {
      return false;
    }
  }

  private int getUniqueSkiers(int resortID, int seasonID, int dayID)
      throws Exception {

    Set<String> uniqueSkiers = new HashSet<>();

    Map<String, AttributeValue> exclusiveStartKey = null;

    do {
      // Use Scan operation with filter expressions
      ScanRequest scanRequest =
          ScanRequest.builder()
              .tableName(DYNAMODB_TABLE_NAME)
              .filterExpression("ResortID = :resortId AND SeasonID = " +
                                ":seasonId AND DayID = :dayId")
              .expressionAttributeValues(Map.of(
                  ":resortId",
                  AttributeValue.builder().n(String.valueOf(resortID)).build(),
                  ":seasonId",
                  AttributeValue.builder().n(String.valueOf(seasonID)).build(),
                  ":dayId",
                  AttributeValue.builder().n(String.valueOf(dayID)).build()))
              .projectionExpression("SkierID")
              .exclusiveStartKey(exclusiveStartKey)
              .build();

      ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

      for (Map<String, AttributeValue> item : scanResponse.items()) {
        String skierID = item.get("SkierID").n();
        uniqueSkiers.add(skierID);
      }

      exclusiveStartKey = scanResponse.lastEvaluatedKey();

    } while (exclusiveStartKey != null && !exclusiveStartKey.isEmpty());

    return uniqueSkiers.size();
  }

  @Override
  public void destroy() {
    super.destroy();
    if (dynamoDbClient != null) {
      dynamoDbClient.close();
    }
  }
}
