/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.examples.quickstart.se;

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

}
