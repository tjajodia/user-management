package com.learnings.self.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.learnings.self.ddb.UserDao;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class UsersHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    final AmazonDynamoDB ddbClient = AmazonDynamoDBClientBuilder.standard().build();
    final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(ddbClient);

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent requestInput, final Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        String httpMethod = requestInput.getHttpMethod();
        String result = "";

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
                    .withStatusCode(200)
                    .withBody(result);
        } catch (Exception e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private String handleMissing(APIGatewayProxyRequestEvent requestInput, Context context) {
        return "";

    }

    private String deleteUser(APIGatewayProxyRequestEvent requestInput, Context context) {
        return "";

    }

    private String updateUser(APIGatewayProxyRequestEvent requestInput, Context context) {
        return "";

    }

    private String createUser(APIGatewayProxyRequestEvent requestInput, Context context) {
        return "";
    }

    private String getUser(APIGatewayProxyRequestEvent requestInput, Context context) {
        Map<String, String> queryStringParameters = requestInput.getQueryStringParameters();
        Map<String, String> pathParameters = requestInput.getPathParameters();
        String pathParametersString = new Gson().toJson(pathParameters);
        String queryStringParametersString = new Gson().toJson(queryStringParameters);
        LambdaLogger logger = context.getLogger();

        logger.log(String.format("getUser pathParams ::%s and queryParams ::%s", pathParametersString, queryStringParametersString));

        if (queryStringParameters != null && queryStringParameters.containsKey("userId")) {
            String userId = queryStringParameters.get("userId");
            UserDao user = dynamoDBMapper.load(UserDao.class, userId);
            return new Gson().toJson(user);

        } else {
            List<UserDao> allUsers = dynamoDBMapper.scan(UserDao.class, new DynamoDBScanExpression());
            return new Gson().toJson(allUsers);
        }


    }
}

