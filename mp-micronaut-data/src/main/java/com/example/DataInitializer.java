package com.example;


import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.runtime.event.annotation.EventListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Logger;

@Singleton
//@TypeHint
public class DataInitializer {
    private final static Logger LOGGER = Logger.getLogger(DataInitializer.class.getName());

    @Inject
    private PostRepository posts;

    @EventListener
    void init(StartupEvent event) {
        LOGGER.info("The application is starting...");
        Post first = Post.of("Hello Helidon", "My first post of Helidon");
        Post second = Post.of("Hello Again, Helidon", "My second post of Helidon");

        this.posts.save(first);
        this.posts.save(second);

        this.posts.findAll().forEach(p -> System.out.println("Post:" + p));
    }

    @EventListener
    void onStop(ShutdownEvent event) {
        LOGGER.info("The application is stopping...");
    }
}
