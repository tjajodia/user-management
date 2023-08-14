package com.learnings.self.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.learnings.self.ddb.UserDao;
import com.learnings.self.infra.ElasticSearchClient;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Handler for requests to Lambda function.
 */
public class UsersHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    final AmazonDynamoDB ddbClient = AmazonDynamoDBClientBuilder.standard().build();
    final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(ddbClient);

    ElasticSearchClient elasticClient = new ElasticSearchClient();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent requestInput, final Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        String httpMethod = requestInput.getHttpMethod();
        Pair<String, Integer> result;

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);


        try {

            switch (httpMethod) {
                case "GET":
                    result = getUser(requestInput, context);
                    break;
                case "POST":
                    result = createUser(requestInput, context);
                    break;
                case "PUT":
                    result = updateUser(requestInput, context);
                    break;
                case "DELETE":
                    result = deleteUser(requestInput, context);
                    break;
                default:
                    result = handleMissing(requestInput, context);
                    break;
            }
            return response
                    .withStatusCode(result.getRight())
                    .withBody(result.getLeft());
        } catch (Exception e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private Pair<String, Integer> handleMissing(APIGatewayProxyRequestEvent requestInput, Context context) {
        return Pair.of("UnKnown Method", 404);
    }

    private Pair<String, Integer> deleteUser(APIGatewayProxyRequestEvent requestInput, Context context) {
        try {
            System.out.println("deleteUser Invoked");
            String requestBody = requestInput.getBody();
            UserDao userInRequest = new Gson().fromJson(requestBody, UserDao.class);
            UserDao existingUser = dynamoDBMapper.load(UserDao.class, userInRequest.getUserId());

            String response = "";

            if (existingUser != null) {
                dynamoDBMapper.delete(existingUser);
                elasticClient.removeItem(existingUser);
                response = "Successfully Deleted User with userId::" + userInRequest.getUserId();
            } else
                response = "Sorry the user was not found with userId::" + userInRequest.getUserId();

            System.out.println("deleteUser Completed");
            return Pair.of(response, 200);

        } catch (Exception exp) {
            System.out.println("deleteUser Invoked");
            exp.printStackTrace();
            // Put the message in DLQ and raise Alarm
            return Pair.of("Something Went Wrong", 500);
        }
    }

    private Pair<String, Integer> updateUser(APIGatewayProxyRequestEvent requestInput, Context context) {
        try {
            System.out.println("updateUser Invoked");
            String requestBody = requestInput.getBody();

            // More Validations Can be Added based on requirements
            UserDao userInRequest = new Gson()
                    .fromJson(requestBody, UserDao.class);

            UserDao existingUser = dynamoDBMapper
                    .load(UserDao.class, userInRequest.getUserId());

            String response = "";

            if (existingUser != null) {
                existingUser.setFirstName(userInRequest.getFirstName());
                existingUser.setLastName(userInRequest.getLastName());
                existingUser.setEmailAddress(userInRequest.getEmailAddress());
                existingUser.setPhoneNumber(userInRequest.getPhoneNumber());
                existingUser.setUserName(userInRequest.getUserName());
                existingUser.setAddress(userInRequest.getAddress());

                dynamoDBMapper.save(existingUser);
                elasticClient.putItem(existingUser);

                response = "Successfully Updated User with userId::" + userInRequest.getUserId();
            } else
                response = "Sorry the user was not found with userId::" + userInRequest.getUserId();
            System.out.println("updateUser Completed");
            return Pair.of(response, 200);

        } catch (Exception exp) {
            System.out.println("updateUser Invoked");
            exp.printStackTrace();
            // Put the message in DLQ and raise Alarm
            return Pair.of("Something Went Wrong", 500);
        }
    }

    private Pair<String, Integer> createUser(APIGatewayProxyRequestEvent requestInput, Context context) {
        try {
            System.out.println("createUser Invoked");
            String requestBody = requestInput.getBody();

            // More Validations Can be Added based on requirements
            UserDao userDao = new Gson().fromJson(requestBody, UserDao.class);

            dynamoDBMapper.save(userDao);
            elasticClient.putItem(userDao);

            String response = new Gson()
                    .toJson(userDao);
            System.out.println("createUser Completed");
            return Pair.of(response, 200);
        } catch (Exception exp) {
            System.out.println("createUser failed");
            exp.printStackTrace();
            // Put the message in DLQ and raise Alarms
            return Pair.of("Something Went Wrong", 500);
        }
    }

    private Pair<String, Integer> getUser(APIGatewayProxyRequestEvent requestInput, Context context) {
        try {
            System.out.println("getUser Invoked");
            Map<String, String> queryStringParameters = requestInput.getQueryStringParameters();

            String response = "";

            if (queryStringParameters != null && queryStringParameters.containsKey("userId")) {

                String userId = queryStringParameters.get("userId");
                UserDao user = elasticClient
                        .getItem(userId)
                        .orElse(dynamoDBMapper  // Mostly this will not be queried
                                .load(UserDao.class, userId));
                response = new Gson()
                        .toJson(user);
            } else {

                // Paginated Support Can be Added as per Requirement
                List<UserDao> allEntries = elasticClient.getAll();
                if (allEntries == null || allEntries.isEmpty()) {
                    allEntries = dynamoDBMapper // Mostly this will not be queried
                            .scan(UserDao.class, new DynamoDBScanExpression());
                }
                response = new Gson()
                        .toJson(allEntries);
            }
            System.out.println("getUser Completed");
            return Pair.of(response, 200);
        } catch (Exception exp) {
            System.out.println("getUser Failed");
            exp.printStackTrace();
            // Put the message in DLQ and raise Alarms
            return Pair.of("Something Went Wrong", 500);
        }
    }
}

