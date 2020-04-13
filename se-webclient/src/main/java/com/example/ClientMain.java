package com.example;

import io.helidon.common.GenericType;
import io.helidon.config.Config;
import io.helidon.media.common.MediaSupport;
import io.helidon.media.jsonb.common.JsonbBodyReader;
import io.helidon.media.jsonb.common.JsonbBodyWriter;
import io.helidon.media.jsonp.common.JsonProcessing;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientRequestBuilder;
import io.helidon.webclient.WebClientServiceRequest;
import io.helidon.webclient.WebClientServiceResponse;
import io.helidon.webclient.metrics.WebClientMetrics;
import io.helidon.webclient.security.WebClientSecurity;
import io.helidon.webclient.spi.WebClientService;
import io.helidon.webclient.tracing.WebClientTracing;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.JsonbBuilder;
import java.util.List;
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
 //                        .registerReader(JSON_PROCESSING.newReader())
 //                        .registerWriter(JSON_PROCESSING.newWriter())
                        .registerReader(JsonbBodyReader.create(JsonbBuilder.create()))
                        .registerWriter(JsonbBodyWriter.create(JsonbBuilder.create()))
                        .build());

        builder.register(new LoggingPostWebClientService());
        //builder.register(WebClientMetrics.counter().build());
        //builder.register(WebClientTracing.create());
        //builder.register(WebClientSecurity.create());

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

class LoggingPostWebClientService implements WebClientService {
    private static final Logger LOGGER = Logger.getLogger(LoggingPostWebClientService.class.getName());

    @Override
    public CompletionStage<WebClientServiceRequest> request(WebClientServiceRequest request) {
        LOGGER.info("Sending requests: " + request);
        return CompletableFuture.completedFuture(request);
    }

    @Override
    public CompletionStage<WebClientServiceResponse> response(WebClientRequestBuilder.ClientRequest request, WebClientServiceResponse response) {
        LOGGER.info("response status: " + response.status());
        return CompletableFuture.completedFuture(response);
    }
}

class PostServiceClient {
    private static final Logger LOGGER = Logger.getLogger(PostServiceClient.class.getName());
    private WebClient webClient;

    public PostServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    //public CompletionStage<JsonArray> getAllPosts() {
        public CompletionStage<List<Post>> getAllPosts() {
        return this.webClient.get()
                .path("/posts")
               // .request(JsonArray.class)
                .request(new GenericType<List<Post>>(){})
                .thenApply(data -> {
                    LOGGER.info("data: " + data);
                    return data;
                });
    }

    //public CompletionStage<JsonObject> getPostById(UUID id) {
        public CompletionStage<Post> getPostById(UUID id) {
        return this.webClient.get()
                .path("/posts/" +id)
                //.request(JsonObject.class)
                .request(Post.class)
                .thenApply(data -> {
                    LOGGER.info("data: " + data);
                    return data;
                });
    }
}