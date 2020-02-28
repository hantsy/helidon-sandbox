
package com.example;

import io.helidon.microprofile.server.Server;
import org.junit.jupiter.api.*;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostServiceTest {
    private final static Logger LOGGER = Logger.getLogger(PostServiceTest.class.getName());

    private Client client;

    private static Server server;

    @BeforeAll
    public static void startTheServer() throws Exception {
        server = Main.startServer();
    }

    @AfterAll
    static void destroyClass() {
        CDI<Object> current = CDI.current();
        ((SeContainer) current).close();
        server.stop();
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
        WebTarget targetGetAll = client.target(URI.create("http://localhost:" + server.port() + path));

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
        WebTarget targetGetNoneExistingPost = client.target(URI.create("http://localhost:" + server.port() + path));

        try (Response resGetNoneExisting = targetGetNoneExistingPost.request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
            assertEquals(404, resGetNoneExisting.getStatus());
        }
    }

}
