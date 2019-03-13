
package com.example;

import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostServiceTest {
    private final static Logger LOGGER = Logger.getLogger(PostServiceTest.class.getName());

    private static WebServer webServer;

    private Client client;

    @BeforeAll
    public static void startTheServer() throws Exception {
        webServer = Main.startServer();
        while (!webServer.isRunning()) {
            Thread.sleep(1 * 1000);
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


    @BeforeEach
    public void beforeEach() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void afterEach() {
        client.close();
    }


    @Test
    public void testGetAllPosts() throws Exception {
        String path = "/posts";
        WebTarget targetGetAll = client.target(URI.create("http://localhost:" + webServer.port() + path));

        try (Response resGetAll = targetGetAll.request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
            assertEquals(200, resGetAll.getStatus());
            List<Post> results = resGetAll.readEntity(new GenericType<List<Post>>() {
            });
            assertTrue(results != null);
            LOGGER.info("results.size()::" + results.size());
            assertTrue(results.size() == 2);
        }
    }


    @Test
    public void testNoneExistingPostById() throws Exception {
        String path = "/posts/noneexisting";
        WebTarget targetGetNoneExistingPost = client.target(URI.create("http://localhost:" + webServer.port() + path));

        try (Response resGetNoneExisting = targetGetNoneExistingPost.request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
            assertEquals(404, resGetNoneExisting.getStatus());
        }
    }

}
