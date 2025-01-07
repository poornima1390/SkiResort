import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

public class SkierApiHandler {
    private final DynamoDbClient dynamoDbClient;
    private static final String DYNAMODB_TABLE_NAME = "Lift_Rides";

    public SkierApiHandler(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = DynamoDbClient.builder()
                .build();
    }

    public Map<String, String> getSkierLiftRideDetails(int resortID, int seasonID, int dayID, int skierID) {

        String sortKeyPrefix = seasonID + "-" + dayID;

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(DYNAMODB_TABLE_NAME)
                .keyConditionExpression("SkierID = :skierID and begins_with(EntryDetail, :sortKeyPrefix)")
                .filterExpression("ResortID = :resortID")
                .expressionAttributeValues(Map.of(
                        ":skierID", AttributeValue.builder().n(String.valueOf(skierID)).build(),
                        ":sortKeyPrefix", AttributeValue.builder().s(sortKeyPrefix).build(),
                        ":resortID", AttributeValue.builder().n(String.valueOf(resortID)).build()
                ))
                .build();

        QueryResponse res = dynamoDbClient.query(queryRequest);



        return res.items().stream()
                .findFirst()
                .map(item -> Map.of(
                        "SkierID", item.get("SkierID").n(),
                        "LiftID", item.get("LiftID").n(),
                        "Time", item.get("Time").n()
                ))
                .orElse(Map.of("message", "ID not found"));
    }

}
