package demo;

import io.helidon.media.jsonb.JsonbSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PostServiceWebClientTest {
    private static WebServer webServer;
    private static final JsonBuilderFactory JSON_BUILDER = Json.createBuilderFactory(Collections.emptyMap());
    private static final JsonObject TEST_JSON_OBJECT;

    static {
        TEST_JSON_OBJECT = JSON_BUILDER.createObjectBuilder()
                .add("greeting", "Hola")
                .build();
    }

    @BeforeAll
    public static void startTheServer() throws Exception {
        webServer = Main.startServer();

        long timeout = 2000; // 2 seconds should be enough to start the server
        long now = System.currentTimeMillis();

        while (!webServer.isRunning()) {
            Thread.sleep(100);
            if ((System.currentTimeMillis() - now) > timeout) {
                Assertions.fail("Failed to start webserver");
            }
        }
    }

    @AfterAll
    public static void stopServer() throws Exception {
        if (webServer != null) {
            webServer.shutdown()
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
        }
    }

    WebClient webClient;

    @BeforeEach
    public void setup() {
        webClient = WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .addMediaSupport(JsonbSupport.create())
                .build();
    }

    @Test
    public void testHelloWorld() throws Exception {
        webClient.get()
                .path("/greet")
                .request(JsonObject.class)
                .thenAccept(jsonObject -> Assertions.assertEquals("Hello World!", jsonObject.getString("message")))
                .toCompletableFuture()
                .get();

        webClient.get()
                .path("/greet/Joe")
                .request(JsonObject.class)
                .thenAccept(jsonObject -> Assertions.assertEquals("Hello Joe!", jsonObject.getString("message")))
                .toCompletableFuture()
                .get();

        webClient.put()
                .path("/greet/greeting")
                .submit(TEST_JSON_OBJECT)
                .thenAccept(response -> Assertions.assertEquals(204, response.status().code()))
                .thenCompose(nothing -> webClient.get()
                        .path("/greet/Joe")
                        .request(JsonObject.class))
                .thenAccept(jsonObject -> Assertions.assertEquals("Hola Joe!", jsonObject.getString("message")))
                .toCompletableFuture()
                .get();

        webClient.get()
                .path("/health")
                .request()
                .thenAccept(response -> Assertions.assertEquals(200, response.status().code()))
                .toCompletableFuture()
                .get();

        webClient.get()
                .path("/metrics")
                .request()
                .thenAccept(response -> Assertions.assertEquals(200, response.status().code()))
                .toCompletableFuture()
                .get();
    }

    @Test
    @DisplayName("Get all posts should return 200")
    public void testGetAllPosts() throws ExecutionException, InterruptedException {
        webClient.get()
                .path("/posts")
                .request(JsonArray.class)
                .thenAccept(arr -> Assertions.assertEquals(2, arr.size()))
                .exceptionally(throwable -> {
                    Assertions.fail(throwable);
                    return null;
                })
                .toCompletableFuture()
                .get();
    }
}
