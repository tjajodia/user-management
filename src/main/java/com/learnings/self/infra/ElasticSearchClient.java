package com.learnings.self.infra;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.learnings.self.constants.Constants;
import com.learnings.self.ddb.UserDao;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.learnings.self.constants.Constants.*;

public class ElasticSearchClient {

    private final RestClient restClient;
    private static final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();

    public ElasticSearchClient() {

        String endpoint = System.getenv(Constants.ES_ENDPOINT);
        AWS4Signer signer = new AWS4Signer();
        signer.setServiceName(ES_SERVICE_NAME);
        signer.setRegionName(DEFAULT_REGION);
        HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(ES_SERVICE_NAME, signer, credentialsProvider);
        this.restClient = RestClient.builder(HttpHost.create(endpoint)).setHttpClientConfigCallback(resInterceptor -> resInterceptor.addInterceptorLast(interceptor)).build();
    }

    public Optional<UserDao> getItem(String userId) throws IOException {
        System.out.println("ElasticSearchClient getItem Invoked");
        Request getRequest = new Request("GET", "/" + Constants.ES_INDEX_NAME + "/_doc/" + userId);
        Response getResponse = restClient.performRequest(getRequest);

        String userString = IOUtils.toString(getResponse.getEntity().getContent());
        JsonObject jsonObject = JsonParser.parseString(userString).getAsJsonObject();

        JsonElement userJsonObject = jsonObject.get("_source");

        Optional<UserDao> response;

        if (!userString.isBlank() && !userString.isEmpty()) {
            UserDao user = new Gson().fromJson(userJsonObject, UserDao.class);
            response = Optional.of(user);
        } else response = Optional.empty();
        System.out.println("ElasticSearchClient getItem Completed");
        return response;
    }

    public void putItem(UserDao user) throws IOException {
        System.out.println("ElasticSearchClient putItem Invoked");
        String userId = user.getUserId();
        Request indexRequest = new Request("PUT", "/" + ES_INDEX_NAME + "/_doc/" + userId);
        String userString = new Gson().toJson(user);
        indexRequest.setJsonEntity(userString);
        Response indexResponse = restClient.performRequest(indexRequest);
        String responseString = IOUtils.toString(indexResponse.getEntity().getContent());
        System.out.printf("ElasticSearchClient putItem Completed with response:: %s", responseString);
    }

    public void removeItem(UserDao user) throws IOException {
        System.out.println("ElasticSearchClient removeItem Invoked");
        String userId = user.getUserId();
        Request indexRequest = new Request("DELETE", "/" + ES_INDEX_NAME + "/_doc/" + userId);
        String userString = new Gson().toJson(user);
        indexRequest.setJsonEntity(userString);
        Response indexResponse = restClient.performRequest(indexRequest);
        String responseString = IOUtils.toString(indexResponse.getEntity().getContent());
        System.out.printf("ElasticSearchClient removeItem Completed with response:: %s", responseString);
    }

    public List<UserDao> getAll() throws IOException {
        System.out.println("getAll removeItem Invoked");
        // Default Size is 10 Can be increase based on the requirements.
        Request indexRequest = new Request("GET", "/" + ES_INDEX_NAME + "/_search/");
        Response indexResponse = restClient.performRequest(indexRequest);
        String responseString = IOUtils.toString(indexResponse.getEntity().getContent());

        List<UserDao> userDaoList = new ArrayList<>();

        JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();
        jsonObject.get("hits").getAsJsonObject().get("hits").getAsJsonArray().forEach(item -> {
            JsonElement obj = item.getAsJsonObject().get("_source");
            UserDao user = new Gson().fromJson(obj, UserDao.class);
            userDaoList.add(user);
        });
        System.out.printf("ElasticSearchClient removeItem Completed with response:: %s", responseString);
        return userDaoList;
    }
}