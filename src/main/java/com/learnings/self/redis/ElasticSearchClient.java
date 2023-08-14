package com.learnings.self.redis;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.learnings.self.ddb.UserDao;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import com.amazonaws.auth.AWS4Signer;

import java.io.IOException;

import java.io.InputStream;
import java.util.Optional;

public class ElasticSearchClient {

    private static final String ES_ENDPOINT = System.getenv("ES_ENDPOINT");
    private static final String INDEX_NAME = "user-mgmt-index";
    private static final String DOC_ID = "1";
    private final RestClient restClient;
    private static final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();

    public ElasticSearchClient() {

        String serviceName = "es";
        String region = "us-east-1";

        AWS4Signer signer = new AWS4Signer();
        signer.setServiceName(serviceName);
        signer.setRegionName(region);
        HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(serviceName, signer, credentialsProvider);
        this.restClient = RestClient.builder(HttpHost.create(ES_ENDPOINT))
                .setHttpClientConfigCallback(x -> x.addInterceptorLast(interceptor))
                .build();
    }

    public Optional<UserDao> getItem(String userId) throws IOException {
        System.out.println("Inside getItem");
        Request getRequest = new Request("GET", "/" + INDEX_NAME + "/_doc/" + DOC_ID);
        Response getResponse = restClient.performRequest(getRequest);
        InputStream inputStream = getResponse.getEntity().getContent();
        String userString = IOUtils.toString(inputStream);
        System.out.println("printing " + userString);

        JsonObject jsonObject = JsonParser
                .parseString(userString)
                .getAsJsonObject();

        JsonElement userJsonObject = jsonObject.get("_source");

        System.out.println("printing json" + userJsonObject);

        Optional<UserDao> response;

        if (!userString.isBlank() && !userString.isEmpty()) {
            UserDao user = new Gson().fromJson(userJsonObject, UserDao.class);
            response = Optional.of(user);
        } else {
            response = Optional.empty();
        }
        System.out.println("Inside getItem Completed");
        return response;
    }

    public void putItem(UserDao user) throws IOException {
        System.out.println("Inside PutItem");
        Request indexRequest = new Request("PUT", "/" + INDEX_NAME + "/_doc/" + DOC_ID);
        String userString = new Gson().toJson(user);
        indexRequest.setJsonEntity(userString);
        Response indexResponse = restClient.performRequest(indexRequest);
        System.out.println("Inside PutItem Completed");
    }
}