package com.example;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.media.common.MediaSupport;
import io.helidon.media.jsonb.common.JsonBinding;
import io.helidon.media.jsonb.common.JsonbBodyReader;
import io.helidon.media.jsonb.common.JsonbBodyWriter;
import io.helidon.media.jsonp.common.JsonProcessing;
import io.helidon.media.jsonp.common.JsonpBodyWriter;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientRequestBuilder;
import io.helidon.webclient.WebClientServiceRequest;
import io.helidon.webclient.WebClientServiceResponse;
import io.helidon.webclient.spi.WebClientService;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.JsonbBuilder;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientMain {
    private static final Logger LOGGER = Logger.getLogger(ClientMain.class.getName());
    private static final JsonProcessing JSON_PROCESSING = JsonProcessing.create();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Config config = Config.create();

        WebClient.Builder builder = WebClient.builder()
                .baseUri("http://localhost:8080")
                .config(config.get("client"))
                .mediaSupport(MediaSupport.builder()
                        .registerDefaults()
                         .registerReader(JSON_PROCESSING.newReader())
                         .registerWriter(JSON_PROCESSING.newWriter())
//                        .registerReader(JsonbBodyReader.create(JsonbBuilder.create()))
//                        .registerWriter(JsonbBodyWriter.create(JsonbBuilder.create()))
                        .build());

        builder.register(new CustomPostWebClientService());

        WebClient webClient = builder.build();

        PostServiceClient client = new PostServiceClient(webClient);
        client.getAllPosts().whenComplete((data, err) -> {
            LOGGER.info("Data of get all posts:" + data);
        }).toCompletableFuture().get();

        UUID id = UUID.randomUUID();
        client.getPostById(id).exceptionally((err) -> {
            LOGGER.log(Level.INFO, "Data of getPostById: {0}, error: {1}", new Object[]{id, err});
            return null;
        }).toCompletableFuture().get();
    }

}

class CustomPostWebClientService implements WebClientService {
    private static final Logger LOGGER = Logger.getLogger(CustomPostWebClientService.class.getName());

    @Override
    public CompletionStage<WebClientServiceRequest> request(WebClientServiceRequest request) {
        LOGGER.info("Sending requests: " + request);
        return CompletableFuture.completedFuture(request);
    }

    @Override
    public CompletionStage<WebClientServiceResponse> response(WebClientRequestBuilder.ClientRequest request, WebClientServiceResponse response) {
        if (response.status().code() == 404) {
            throw new RuntimeException("404 exception");
        }

        return CompletableFuture.completedFuture(response);
    }
}

class PostServiceClient {
    private static final Logger LOGGER = Logger.getLogger(PostServiceClient.class.getName());
    private WebClient webClient;

    public PostServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public CompletionStage<JsonArray> getAllPosts() {
        return this.webClient.get()
                .path("/posts")
                .request(JsonArray.class)
                .thenApply(data -> {
                    LOGGER.info("data: " + data);
                    return data;
                });
    }

    public CompletionStage<JsonObject> getPostById(UUID id) {
        return this.webClient.get()
                .path("/posts/" +id)
                .request(JsonObject.class)
                .thenApply(data -> {
                    LOGGER.info("data: " + data);
                    return data;
                });
    }
}