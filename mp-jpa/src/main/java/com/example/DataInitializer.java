package com.example;


import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Logger;

@ApplicationScoped
public class DataInitializer {
    private final static Logger LOGGER = Logger.getLogger(DataInitializer.class.getName());

    @Inject
    private PostRepository posts;

    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOGGER.info("The application is starting...");
        Post first = Post.of("Hello Helidon", "My first post of Helidon");
        Post second = Post.of("Hello Again, Helidon", "My second post of Helidon");

        this.posts.save(first);
        this.posts.save(second);

        this.posts.findAll().forEach(p -> System.out.println("Post:" + p));
    }

    void onStop(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        LOGGER.info("The application is stopping...");
    }
}
