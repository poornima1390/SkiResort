import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Map;

public class SkierApiHandler {
    private final DynamoDbClient dynamoDbClient;
    private static final String DYNAMODB_TABLE_NAME = "LiftRides";

    public SkierApiHandler(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = DynamoDbClient.builder()
                .build();
    }

    public Map<String, String> getSkierLiftRideDetails(int resortID, int seasonID, int dayID, int skierID) {

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(DYNAMODB_TABLE_NAME)
                .filterExpression("ResortID = :resortID and SeasonID = :seasonID and DayID = :dayID and SkierID = :skierID")
                .expressionAttributeValues(Map.of(
                        ":resortID", AttributeValue.builder().n(String.valueOf(resortID)).build(),
                        ":seasonID", AttributeValue.builder().n(String.valueOf(seasonID)).build(),
                        ":dayID", AttributeValue.builder().n(String.valueOf(dayID)).build(),
                        ":skierID", AttributeValue.builder().n(String.valueOf(skierID)).build()
                ))
                .build();


        ScanResponse res = dynamoDbClient.scan(scanRequest);


        return res.items().stream()
                .findFirst()
                .map(item -> Map.of(
                        "SkierID", item.get("SkierID").n(),
                        "LiftID", item.get("LiftID").n(),
                        "Time", item.get("Time").n()
                ))
                .orElseThrow(() -> new RuntimeException("No data found "));
    }

}
