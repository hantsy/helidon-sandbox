package com.example;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import java.util.logging.Logger;

import static javax.interceptor.Interceptor.Priority.LIBRARY_BEFORE;

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

    // See https://github.com/oracle/helidon/issues/3009 and https://github.com/oracle/helidon/issues/2630
    private static void helidon3009Workaround(@Observes @Initialized(ApplicationScoped.class) @Priority(LIBRARY_BEFORE) Object init,
                                              @Any Instance<EntityManagerFactory> emfProxies) {
        for (EntityManagerFactory emfProxy : emfProxies) {
            emfProxy.isOpen();
        }
    }

    void onStop(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        LOGGER.info("The application is stopping...");
    }
}
