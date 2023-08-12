package com.learnings.self.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

import static com.learnings.self.constants.Constants.USER_TABLE;

/**
 * Handler for requests to Lambda function.
 */
public class UsersHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {


    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {


        Map<String, String> queryStringParameters = input.getQueryStringParameters();
        String userId = queryStringParameters.getOrDefault("userId", "default");
        final AmazonDynamoDB ddbClient = AmazonDynamoDBClientBuilder.defaultClient();

        Map<String, AttributeValue> getArributeMap = new HashMap<>();
        getArributeMap.put("userId", new AttributeValue(userId));

        GetItemRequest getItemRequest = new GetItemRequest()
                .withKey(getArributeMap)
                .withTableName(USER_TABLE);

        GetItemResult itemResult = ddbClient.getItem(getItemRequest);
        Map<String, AttributeValue> resultMap = itemResult.getItem();

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        String firstName = resultMap.get("firstName").toString();
        String lastName = resultMap.get("lastName").toString();
        String emailAddress = resultMap.get("emailAddress").toString();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            String output = String.format("{ \"name\": \" %s %s \", \"email\": \"%s\" }",firstName,lastName,emailAddress);

            return response
                    .withStatusCode(200)
                    .withBody(output);
        } catch (Exception e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }
}
